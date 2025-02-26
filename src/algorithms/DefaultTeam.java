package algorithms;

import java.awt.Point;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultTeam {
    private boolean showLog = true; // Variable pour activer/désactiver les logs
    private boolean showDetails = false;
    private double precision = 2.5;

    public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
        ArrayList<Point> result = new ArrayList<>();
        Map<Point, List<Point>> neighborsMap = calculateNeighborsMap(points, edgeThreshold);

        File resultsDirectory = new File("results/");
        if (!resultsDirectory.exists()) {
            resultsDirectory.mkdir();
        }
        File[] files = resultsDirectory.listFiles();
        if (files != null) {
            for (File fileEntry : files) {

                result = readFromFile(fileEntry.getName());
                //log(fileEntry.getName());
                assert result != null;
                if (!result.isEmpty())
                    if (isDominatingSet(result, points, edgeThreshold, neighborsMap)) {
                        log("Load from file (" + fileEntry.getName() +") : " + result.size() + " dominated points.");
                        return result;
                    }
            }
        }

        log("Start of Dominating Set calculation...");

        result.clear();
        result = gloutonDominatingSet(points, edgeThreshold);
        result = remove2add1(result, points, edgeThreshold);
        result = remove3add2(result, points, edgeThreshold);
        saveToFile("result", result);

        log("End of Dominating Set calculation : " + result.size() + " dominated points.");
        return result;
    }


    private ArrayList<Point> gloutonDominatingSet(ArrayList<Point> points, int edgeThreshold) {
        long startTime = System.currentTimeMillis();
        log("[Glouton] Start of the algorithm...");

        Map<Point, List<Point>> neighborsMap = calculateNeighborsMap(points, edgeThreshold);
        Map<Point, List<Point>> finalNeighborsMap1 = neighborsMap;
        PriorityQueue<Point> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(pt -> finalNeighborsMap1.get(pt).size()).reversed());

        ArrayList<Point> dominatedSet = getAlonePoints(neighborsMap);
        HashSet<Point> dominated = new HashSet<>(dominatedSet);
        ArrayList<Point> newPoints = new ArrayList<>(points);
        newPoints.removeAll(dominatedSet);

        while (!dominated.containsAll(points)) {
            for (Point point : newPoints) {
                if (neighborsMap.get(point).isEmpty()) {
                    dominated.add(point);
                    dominatedSet.add(point);
                } else {
                    priorityQueue.add(point);
                }
            }

            Point p = priorityQueue.poll();

            if (!dominated.contains(p)) {
                dominated.add(p);
                dominatedSet.add(p);
                dominated.addAll(neighborsMap.get(p));

                newPoints.removeAll(dominated);
                neighborsMap = calculateNeighborsMap(newPoints, edgeThreshold);
                priorityQueue.clear();
                Map<Point, List<Point>> finalNeighborsMap = neighborsMap;
                priorityQueue = new PriorityQueue<>(Comparator.comparingInt(pt -> finalNeighborsMap.get(pt).size()).reversed());
            }
        }

        dominatedSet = cleanDominatingSet(dominatedSet, points, edgeThreshold);

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
        return vertices.stream().filter(point -> point.distance(p) < edgeThreshold && !point.equals(p)).collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<Point> remove2add1(ArrayList<Point> dominatedSet, ArrayList<Point> points, int edgeThreshold) {
        long startTime = System.currentTimeMillis();
        log("[remove2add1] Start of the algorithm...");
        int deletedDominatedPoints = 0;

        Map<Point, List<Point>> neighborsMap = calculateNeighborsMap(points, edgeThreshold);

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
                        if (p1.distance(newPoint) > edgeThreshold || p2.distance(newPoint) > edgeThreshold) {
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
                            detail_log("[remove2add1] Replaced points " + p1.toString() + " and " + p2.toString() + " with point " + newPoint.toString());
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

    private ArrayList<Point> remove3add2(ArrayList<Point> dominatedSet, ArrayList<Point> points, int edgeThreshold) {
        long startTime = System.currentTimeMillis();
        log("[remove3add2] Start of the algorithm...");
        boolean improved = true;
        int deletedDominatedPoints = 0;

        ArrayList<Point> newDominatedSet = new ArrayList<>();
        Map<Point, List<Point>> neighborsMap = calculateNeighborsMap(points, edgeThreshold);
        Set<Point> dominatedSetHash = new HashSet<>(dominatedSet);
        Set<Point> pointsHash = new HashSet<>(points);

        int index = 0;

        while (improved) {
            improved = false;
            outerLoop:
            for (int i = index; i < dominatedSetHash.size(); i++) {
                Point p1 = dominatedSet.get(i);
                ArrayList<Point> p1Neighbors = neighbor(p1, dominatedSet, 2 * edgeThreshold);

                detail_log("[remove3add2] Treated : " + i + "/" + dominatedSetHash.size());

                for (int j = 0; j < p1Neighbors.size(); j++) {
                    Point p2 = p1Neighbors.get(j);
                    ArrayList<Point> p2Neighbors = neighbor(p2, dominatedSet, 2 * edgeThreshold);


                    for (int k = 0; k < p2Neighbors.size(); k++) {
                        Point p3 = p2Neighbors.get(k);


                        if (p3.equals(p1)) {
                            continue;
                        }

                        ArrayList<Point> allNeighborPoints = new ArrayList<>();
                        allNeighborPoints.addAll(neighbor(p1, points, 2 * edgeThreshold));
                        allNeighborPoints.addAll(neighbor(p2, points, 2 * edgeThreshold));
                        allNeighborPoints.addAll(neighbor(p3, points, 2 * edgeThreshold));

                        for (Point newPoint1 : allNeighborPoints) {

                            if (dominatedSetHash.contains(newPoint1)) {
                                continue;
                            }

                            if (p1.distance(newPoint1) > precision * edgeThreshold
                                    || p2.distance(newPoint1) > precision * edgeThreshold
                                    || p3.distance(newPoint1) > precision * edgeThreshold) {
                                continue;
                            }

                            for (Point newPoint2 : allNeighborPoints) {

                                if (newPoint1.equals(newPoint2) || dominatedSetHash.contains(newPoint2)) {
                                    continue;
                                }

                                if (p1.distance(newPoint2) > precision * edgeThreshold
                                        || p2.distance(newPoint2) > precision * edgeThreshold
                                        || p3.distance(newPoint2) > precision * edgeThreshold) {
                                    continue;
                                }

                                newDominatedSet = new ArrayList<>(dominatedSet);
                                newDominatedSet.remove(p1);
                                newDominatedSet.remove(p2);
                                newDominatedSet.remove(p3);
                                newDominatedSet.add(newPoint1);
                                newDominatedSet.add(newPoint2);
                                if (isDominatingSet(newDominatedSet, points, edgeThreshold, neighborsMap)) {
                                    dominatedSet = newDominatedSet;
                                    dominatedSetHash = new HashSet<>(dominatedSet);
                                    improved = true;
                                    deletedDominatedPoints++;
                                    index = i + 1;
                                    detail_log("[remove3add2] Replaced points " + p1 + ", " + p2 + ", " + p3 + " with points " + newPoint1 + ", " + newPoint2);
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
            ArrayList<Point> pointNeighborsMap = (ArrayList<Point>) neighborsMap.get(p);
            if (pointNeighborsMap != null) {
                dominated.addAll(pointNeighborsMap);
            }
        }
        return dominated.containsAll(points);
    }

    private ArrayList<Point> cleanDominatingSet(ArrayList<Point> dominatingSet, ArrayList<Point> points, int edgeThreshold) {
        if (dominatingSet == null) {
            return new ArrayList<>();
        }
        ArrayList<Point> cleanDominatingSet = new ArrayList<>(dominatingSet);
        Map<Point, List<Point>> neighborsMap = calculateNeighborsMap(points, edgeThreshold);
        for (Point p : dominatingSet) {
            cleanDominatingSet.remove(p);
            if (!isDominatingSet(cleanDominatingSet, points, edgeThreshold, neighborsMap)) {
                cleanDominatingSet.add(p);
            }
        }

        log("[Clean] From " + dominatingSet.size() + " to " + cleanDominatingSet.size() + " dominated points.");
        return cleanDominatingSet;
    }


    private ArrayList<Point> getAlonePoints(Map<Point, List<Point>> neighborsMap) {
        return neighborsMap.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
    }

    private Point getGreatestDegreePoint(ArrayList<Point> points, int edgeThreshold) {
        final Comparator<Point> comp = Comparator.comparingInt(p -> neighbor(p, points, edgeThreshold).size());
        return points.stream().max(comp).orElseThrow(IllegalArgumentException::new);
    }

    private void log(String message) {
        if (showLog) {
            System.out.println(message);
        }
    }

    private void detail_log(String message) {
        if (showDetails) {
            System.out.println(message);
        }
    }


    private void saveToFile(String filename, ArrayList<Point> result) {
        String directory = "./results/";
        int index = 0;
        File file = new File(directory + filename + Integer.toString(index) + ".points");
        while (file.exists()) {
            index++;
            file = new File(directory + filename + Integer.toString(index) + ".points");
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
        //log("Printed to file: " + filename);
    }

    private ArrayList<Point> readFromFile(String filename) {
        String directory = "./results/";
        ArrayList<Point> points = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(directory + filename)))) {
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
        return points.isEmpty() ? null : points;
    }


}
