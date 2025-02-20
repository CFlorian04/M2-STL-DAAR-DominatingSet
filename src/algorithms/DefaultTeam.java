package algorithms;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

public class DefaultTeam {
    private boolean showLog = true; // Variable pour activer/désactiver les logs

    public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
        ArrayList<Point> result = new ArrayList<>();
        HashSet<Point> dominated = new HashSet<>();

        log("Starting dominating set calculation...");

        // Construction initiale de l'ensemble dominant
        while (!dominated.containsAll(points)) {
            Point maxPoint = null;
            int maxNeighbors = 0;

            for (Point p : points) {
                if (!dominated.contains(p)) {
                    int neighbors = countNeighbors(p, points, edgeThreshold, dominated);
                    if (neighbors > maxNeighbors) {
                        maxNeighbors = neighbors;
                        maxPoint = p;
                    }
                }
            }

            if (maxPoint != null) {
                result.add(maxPoint);
                dominated.add(maxPoint);
                for (Point p : points) {
                    if (p.distance(maxPoint) < edgeThreshold) {
                        dominated.add(p);
                    }
                }
                log("Added point: " + maxPoint.toString() + " with " + maxNeighbors + " neighbors.");
            }
        }

        log("Initial dominating set constructed.");

        // Optimisation avec remove2add1
        result = remove2add1(result, points, edgeThreshold);

        log("Optimization with remove2add1 completed.");

        return result;
    }

    private int countNeighbors(Point p, ArrayList<Point> points, int edgeThreshold, HashSet<Point> dominated) {
        int count = 0;
        for (Point q : points) {
            if (!dominated.contains(q) && p.distance(q) < edgeThreshold) {
                count++;
            }
        }
        return count;
    }

    private ArrayList<Point> remove2add1(ArrayList<Point> dominatingSet, ArrayList<Point> points, int edgeThreshold) {
        boolean improved = true;
        while (improved) {
            improved = false;
            outerLoop:
            for (int i = 0; i < dominatingSet.size(); i++) {
                for (int j = i + 1; j < dominatingSet.size(); j++) {
                    Point p1 = dominatingSet.get(i);
                    Point p2 = dominatingSet.get(j);

                    if (p1.distance(p2) > 2 * edgeThreshold) {
                        continue;
                    }

                    for (Point p : points) {

                        if(p1.distance(p) > 2 * edgeThreshold || p2.distance(p) > 2 * edgeThreshold) {continue;}

                        if (!dominatingSet.contains(p)) {
                            ArrayList<Point> newDominatingSet = new ArrayList<>(dominatingSet);
                            newDominatingSet.remove(p1);
                            newDominatingSet.remove(p2);
                            newDominatingSet.add(p);
                            if (isDominatingSet(newDominatingSet, points, edgeThreshold)) {
                                dominatingSet = newDominatingSet;
                                improved = true;
                                log("Replaced points " + p1.toString() + " and " + p2.toString() + " with point " + p.toString());
                                break outerLoop; // Sortir des boucles imbriquées dès qu'une amélioration est trouvée
                            }
                        }
                    }
                }
            }
        }
        return dominatingSet;
    }

    private boolean isDominatingSet(ArrayList<Point> dominatingSet, ArrayList<Point> points, int edgeThreshold) {
        HashSet<Point> dominated = new HashSet<>(dominatingSet);
        for (Point p : dominatingSet) {
            for (Point q : points) {
                if (p.distance(q) < edgeThreshold) {
                    dominated.add(q);
                }
            }
        }
        return dominated.containsAll(points);
    }

    // FILE PRINTER
    private void saveToFile(String filename, ArrayList<Point> result) {
        int index = 0;
        File file = new File(filename + Integer.toString(index) + ".points");
        while (file.exists()) {
            index++;
            file = new File(filename + Integer.toString(index) + ".points");
        }
        printToFile(file.getPath(), result);
        log("Saved to file: " + file.getPath());
    }

    private void printToFile(String filename, ArrayList<Point> points) {
        try (PrintStream output = new PrintStream(new FileOutputStream(filename))) {
            for (Point p : points) {
                output.println(Integer.toString((int) p.getX()) + " " + Integer.toString((int) p.getY()));
            }
        } catch (FileNotFoundException e) {
            System.err.println("I/O exception: unable to create " + filename);
        }
        log("Printed to file: " + filename);
    }

    // FILE LOADER
    private ArrayList<Point> readFromFile(String filename) {
        ArrayList<Point> points = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
            String line;
            while ((line = input.readLine()) != null) {
                String[] coordinates = line.split("\\s+");
                points.add(new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found.");
        } catch (IOException e) {
            System.err.println("Exception: interrupted I/O.");
        }
        log("Read from file: " + filename);
        return points;
    }

    // Méthode pour afficher les logs
    private void log(String message) {
        if (showLog) {
            System.out.println(message);
        }
    }

    // Méthode pour activer/désactiver les logs
    public void setShowLog(boolean showLog) {
        this.showLog = showLog;
    }
}
