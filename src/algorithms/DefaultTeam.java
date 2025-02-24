package algorithms;

import java.awt.Point;
import java.io.*;
import java.util.*;

public class DefaultTeam {
    private boolean showLog = true; // Variable pour activer/désactiver les logs

    public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
        log("Start of Dominating Set calculation...");

        // Calculer la carte des voisins une seule fois et la réutiliser
        Map<Point, List<Point>> neighborsMap = calculateNeighborsMap(points, edgeThreshold);

        ArrayList<Point> result = gloutonDominatingSet(points, edgeThreshold, neighborsMap);
        result = remove2add1(result, points, edgeThreshold, neighborsMap);
        //result = remove3add2(result, points, edgeThreshold, neighborsMap);

        log("End of Dominating Set calculation...");
        return result;
    }

    private ArrayList<Point> gloutonDominatingSet(ArrayList<Point> points, int edgeThreshold, Map<Point, List<Point>> neighborsMap) {
        long startTime = System.currentTimeMillis();
        log("[Glouton] Start of the algorithm...");
        ArrayList<Point> dominatedSet = new ArrayList<>();
        HashSet<Point> dominated = new HashSet<>();
        ArrayList<Point> newPoints = new ArrayList<>(points);
        Map<Point, List<Point>> newNeighborsMap = neighborsMap;
        PriorityQueue<Point> priorityQueue = new PriorityQueue<>(
                Comparator.comparingInt(pt -> neighborsMap.get(pt).size()).reversed()
        );


        while (!dominated.containsAll(points)) {
            // Initialisation de la file de priorité
            for (Point point : newPoints) {
                if (newNeighborsMap.get(point).isEmpty()) {
                    dominated.add(point);
                    dominatedSet.add(point);
                } else {
                    priorityQueue.add(point);
                }
            }

            Point p = priorityQueue.poll();

            log("[Glouton] Point neighbor size: " + neighborsMap.get(p).size());

            if (!dominated.contains(p)) {
                dominated.add(p);
                dominatedSet.add(p);
                dominated.addAll(neighborsMap.get(p));

                newPoints.removeAll(dominated);
                newNeighborsMap = calculateNeighborsMap(newPoints, edgeThreshold);
                Map<Point, List<Point>> finalNewNeighborsMap = newNeighborsMap;

                priorityQueue.clear();
                priorityQueue = new PriorityQueue<>(
                        Comparator.comparingInt(pt -> finalNewNeighborsMap.get(pt).size()).reversed()
                );

            }

        }



        long endTime = System.currentTimeMillis();

        log("[Glouton] " + dominatedSet.size() + " dominated points.");
        log("[Glouton] End of the algorithm (" + (endTime - startTime) + "ms).");
        return dominatedSet;
    }


    private Map<Point, List<Point>> calculateNeighborsMap(ArrayList<Point> points, int edgeThreshold) {
        Map<Point, List<Point>> neighborsMap = new HashMap<>();
        for (Point point : points) {
            neighborsMap.put(point, neighbor(point, points, edgeThreshold));
        }
        return neighborsMap;
    }

    private ArrayList<Point> neighbor(Point p, ArrayList<Point> vertices, int edgeThreshold) {
        ArrayList<Point> result = new ArrayList<>();
        for (Point point : vertices) {
            if (point.distance(p) < edgeThreshold && !point.equals(p)) {
                result.add((Point) point.clone());
            }
        }
        return result;
    }

    private ArrayList<Point> remove2add1(ArrayList<Point> dominatedSet, ArrayList<Point> points, int edgeThreshold, Map<Point, List<Point>> neighborsMap) {
        long startTime = System.currentTimeMillis();
        log("[remove2add1] Start of the algorithm...");
        int deletedDominatedPoints = 0;

        boolean improved = true;
        while (improved) {
            improved = false;
            outerLoop:
            for (int i = 0; i < dominatedSet.size() - 1; i++) {
                Point p1 = dominatedSet.get(i);
                for (int j = i + 1; j < dominatedSet.size(); j++) {
                    Point p2 = dominatedSet.get(j);
                    if (p1.distance(p2) > 2 * edgeThreshold) {
                        continue;
                    }
                    for (Point newPoint : points) {
                        if (dominatedSet.contains(newPoint)) {
                            continue;
                        }
                        if (p1.distance(newPoint) > 2 * edgeThreshold || p2.distance(newPoint) > 2 * edgeThreshold) {
                            continue;
                        }
                        ArrayList<Point> newDominatedSet = new ArrayList<>(dominatedSet);
                        newDominatedSet.remove(p1);
                        newDominatedSet.remove(p2);
                        newDominatedSet.add(newPoint);
                        if (isDominatingSet(newDominatedSet, points, edgeThreshold, neighborsMap)) {
                            dominatedSet = newDominatedSet;
                            improved = true;
                            deletedDominatedPoints++;
                            //log("[remove2add1] Replaced points " + p1.toString() + " and " + p2.toString() + " with point " + newPoint.toString());
                            break outerLoop;
                        }
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        log("[remove2add1] " + dominatedSet.size() + " dominated points (" + deletedDominatedPoints + " deleted).");
        log("[remove2add1] End of the algorithm (" + (endTime - startTime) + "ms).");
        return dominatedSet;
    }

    private ArrayList<Point> remove3add2(ArrayList<Point> dominatedSet, ArrayList<Point> points, int edgeThreshold, Map<Point, List<Point>> neighborsMap) {
        long startTime = System.currentTimeMillis();
        log("[remove3add2] Start of the algorithm...");
        boolean improved = true;
        int deletedDominatedPoints = 0;

        while (improved) {
            improved = false;
            outerLoop:
            for (int i = 0; i < dominatedSet.size() - 2; i++) {
                Point p1 = dominatedSet.get(i);
                for (int j = i + 1; j < dominatedSet.size() - 1; j++) {
                    Point p2 = dominatedSet.get(j);
                    if (p1.distance(p2) > 4 * edgeThreshold) {
                        continue;
                    }
                    for (int k = j + 1; k < dominatedSet.size(); k++) {
                        Point p3 = dominatedSet.get(k);

                        if (p2.distance(p3) > 4 * edgeThreshold || p3.distance(p1) > 4 * edgeThreshold) {
                            continue;
                        }
                        for (Point newPoint1 : points) {
                            if (dominatedSet.contains(newPoint1)) {
                                continue;
                            }
                            if (p1.distance(newPoint1) > 4 * edgeThreshold || p2.distance(newPoint1) > 4 * edgeThreshold || p3.distance(newPoint1) > 4 * edgeThreshold) {
                                continue;
                            }
                            for (Point newPoint2 : points) {
                                if (newPoint1.equals(newPoint2) || dominatedSet.contains(newPoint2)) {
                                    continue;
                                }
                                double distP1New2 = p1.distance(newPoint2);
                                double distP2New2 = p2.distance(newPoint2);
                                double distP3New2 = p3.distance(newPoint2);
                                if (distP1New2 > 4 * edgeThreshold || distP2New2 > 4 * edgeThreshold || distP3New2 > 4 * edgeThreshold) {
                                    continue;
                                }
                                ArrayList<Point> newDominatedSet = new ArrayList<>(dominatedSet);
                                newDominatedSet.remove(p1);
                                newDominatedSet.remove(p2);
                                newDominatedSet.remove(p3);
                                newDominatedSet.add(newPoint1);
                                newDominatedSet.add(newPoint2);
                                if (isDominatingSet(newDominatedSet, points, edgeThreshold, neighborsMap)) {
                                    dominatedSet = newDominatedSet;
                                    improved = true;
                                    deletedDominatedPoints++;
                                    //log("[remove3add2] Replaced points " + p1.toString() + ", " + p2.toString() + ", " + p3.toString() + " with point " + newPoint1.toString() + ", " + newPoint2.toString());
                                    break outerLoop;
                                }
                            }
                        }
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        log("[remove3add2] " + dominatedSet.size() + " dominated points (" + deletedDominatedPoints + " deleted).");
        log("[remove3add2] End of the algorithm (" + (endTime - startTime) + "ms).");
        return dominatedSet;
    }

    private boolean isDominatingSet(ArrayList<Point> dominatingSet, ArrayList<Point> points, int edgeThreshold, Map<Point, List<Point>> neighborsMap) {
        HashSet<Point> dominated = new HashSet<>(dominatingSet);
        for (Point p : dominatingSet) {
            dominated.addAll(neighborsMap.get(p)); // [AI] Utilisation de la carte des voisins pour éviter de recalculer les voisins
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
            System.err.println("Input file not found: " + filename);
        } catch (IOException e) {
            System.err.println("Exception: interrupted I/O while reading " + filename);
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
}
