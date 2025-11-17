package org.example;

import java.util.Arrays;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    // Enum
    enum MusicStyle {
        POP, ROCK, JAZZ
    }

    // Базовий клас
    abstract static class MusicComposition {

        private final String title;
        private final int durationSeconds;
        private final MusicStyle style;

        /**
         * Створює композицію
         *
         * @param title           назва композиції
         * @param durationSeconds тривалість у секундах
         * @param style           стиль музики
         */
        public MusicComposition(String title, int durationSeconds, MusicStyle style) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title must be non-empty.");
            }
            if (durationSeconds <= 0) {
                throw new IllegalArgumentException("Duration must be positive.");
            }
            if (style == null) {
                throw new IllegalArgumentException("Style must be provided.");
            }
            this.title = title;
            this.durationSeconds = durationSeconds;
            this.style = style;
        }

        public String getTitle() {
            return title;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public MusicStyle getStyle() {
            return style;
        }

        @Override
        public String toString() {
            return title + " (" + style + ", " + durationSeconds + " sec)";
        }

        /**
         * Серіалізує запис композиції у простому текстовому форматі: title|style|duration
         * @return серіалізований рядок
         */
        public String serialize() {
            return escape(title) + "|" + style.name() + "|" + durationSeconds;
        }

        /**
         * Примітивне екранування символу '|' у назві.
         */
        private static String escape(String s) {
            return s.replace("|", "\\|");
        }

        /**
         * Розекранування назви.
         */
        private static String unescape(String s) {
            return s.replace("\\|", "|");
        }

        /**
         * Десеріалізує композицію з частин; повертає MusicComposition конкретного підкласу.
         *
         * @param parts масив частин [title, style, duration]
         * @return об'єкт MusicComposition
         */
        public static MusicComposition deserialize(String[] parts) {
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid serialized composition format.");
            }
            String title = unescape(parts[0]);
            MusicStyle style = MusicStyle.valueOf(parts[1]);
            int duration = Integer.parseInt(parts[2]);
            switch (style) {
                case POP: return new PopSong(title, duration);
                case ROCK: return new RockSong(title, duration);
                case JAZZ: return new JazzSong(title, duration);
                default: throw new IllegalArgumentException("Unknown style: " + style);
            }
        }
    }

    //Нащадки
    static class PopSong extends MusicComposition {
        public PopSong(String title, int durationSeconds) {
            super(title, durationSeconds, MusicStyle.POP);
        }
    }

    static class RockSong extends MusicComposition {
        public RockSong(String title, int durationSeconds) {
            super(title, durationSeconds, MusicStyle.ROCK);
        }
    }

    static class JazzSong extends MusicComposition {
        public JazzSong(String title, int durationSeconds) {
            super(title, durationSeconds, MusicStyle.JAZZ);
        }
    }

    //Виняток
    static class CompositionNotFoundException extends Exception {
        public CompositionNotFoundException(String message) {
            super(message);
        }
    }

    // Клас Альбом
    static class Album {

        private final MusicComposition[] compositions;

        public Album(MusicComposition[] compositions) {
            if (compositions == null || compositions.length == 0) {
                throw new IllegalArgumentException("Album must contain at least one composition.");
            }
            this.compositions = compositions;
        }

        /**
         * Обчислення загальної тривалість альбомуу.
         *
         * @return сумарна тривалість
         */
        public int getTotalDuration() {
            int sum = 0;
            for (MusicComposition track : compositions) {
                sum += track.getDurationSeconds();
            }
            return sum;
        }

        /**
         * Сортує композиції за MusicStyle.
         */
        public void sortByStyle() {
            Arrays.sort(compositions, (a, b) -> a.getStyle().compareTo(b.getStyle()));
        }

        /**
         * Знаходить першу композицію у вказаному діапазоні тривалості.
         *
         * @param min мінімум (включно)
         * @param max максимум (включно)
         * @return знайдена композиція
         * @throws CompositionNotFoundException якщо нічого не знайдено
         */
        public MusicComposition findByDuration(int min, int max)
                throws CompositionNotFoundException {

            if (min < 0 || max < 0 || min > max) {
                throw new IllegalArgumentException("Invalid duration range.");
            }

            for (MusicComposition track : compositions) {
                int d = track.getDurationSeconds();
                if (d >= min && d <= max) {
                    return track;
                }
            }

            throw new CompositionNotFoundException(
                    "No composition found in duration range " + min + "–" + max);
        }

        public void printAlbum() {
            for (MusicComposition track : compositions) {
                System.out.println(track);
            }
        }

        /**
         * Записує альбом у текстовий файл за простим форматом:
         * перший рядок — кількість треків,
         * потім кожен трек у форматі title|style|duration
         *
         * @param path шлях до файлу (наприклад "album.txt")
         * @throws IOException при помилці запису
         */
        public void saveToFile(String path) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                writer.write(String.valueOf(compositions.length));
                writer.newLine();
                for (MusicComposition track : compositions) {
                    writer.write(track.serialize());
                    writer.newLine();
                }
            }
        }

        /**
         * Статичний хелпер для завантаження альбому з файлу.
         *
         * @param path шлях до файлу
         * @return Album
         * @throws IOException при помилці читання
         */
        public static Album loadFromFile(String path) throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("File is empty.");
                }
                int n = Integer.parseInt(line.trim());
                if (n <= 0) {
                    throw new IOException("Invalid track count: " + n);
                }
                MusicComposition[] arr = new MusicComposition[n];
                for (int i = 0; i < n; i++) {
                    String item = reader.readLine();
                    if (item == null) {
                        throw new IOException("Unexpected end of file.");
                    }

                    String[] parts = item.split("\\|");
                    arr[i] = MusicComposition.deserialize(parts);
                }
                return new Album(arr);
            }
        }
    }

    //Виконання програми
    public static void main(String[] args) {
        try {
            MusicComposition[] tracks = {
                    new RockSong("Numb", 185),
                    new PopSong("Blinding Lights", 200),
                    new JazzSong("Autumn Leaves", 240),
                    new PopSong("Levitating", 205),
                    new RockSong("In the End", 215)
            };

            Album album = new Album(tracks);

            System.out.println("Album:");
            album.printAlbum();

            album.sortByStyle();
            System.out.println("\nAlbum after sorting:");
            album.printAlbum();

            System.out.println("\nTotal duration: " + album.getTotalDuration() + " seconds");

            System.out.println("\nSearching for track with duration 190–210...");
            MusicComposition found = album.findByDuration(190, 210);
            System.out.println("Found: " + found);

            //запис на диск
            String path = "album.txt";
            album.saveToFile(path);
            System.out.println("\nAlbum saved to file: " + path);

            // приклад завантаження з диска
            Album loaded = Album.loadFromFile(path);
            System.out.println("\nLoaded album from file:");
            loaded.printAlbum();

        } catch (CompositionNotFoundException e) {
            System.err.println("Search error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Argument error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
