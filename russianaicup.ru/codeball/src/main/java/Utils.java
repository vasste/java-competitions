import model.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Utils {
   public static double SMALL_DOUBLE = 0.0005;

   private static Map<VehicleType, List<VehicleType>> unitsCounterMatrix = new HashMap<>();

   static {
      unitsCounterMatrix.put(VehicleType.FIGHTER, Arrays.asList(VehicleType.HELICOPTER));
      unitsCounterMatrix.put(VehicleType.HELICOPTER, Arrays.asList(VehicleType.ARRV, VehicleType.TANK));
      unitsCounterMatrix.put(VehicleType.TANK, Arrays.asList(VehicleType.ARRV, VehicleType.IFV));
      unitsCounterMatrix.put(VehicleType.IFV, Arrays.asList(VehicleType.ARRV, VehicleType.FIGHTER,
              VehicleType.HELICOPTER));
      unitsCounterMatrix.put(VehicleType.ARRV, new ArrayList<>());
   }

   public static boolean equals(double d1, double d2) {
      return Math.abs(d1 - d2) < SMALL_DOUBLE;
   }

   /**
    * Normalize given angle to interval [-PI, PI]
    *
    * @param angle any angle
    * @return angle in interval [-PI, PI]
    */
   public static double normalizeAngle(double angle) {
      double resAngle = angle;
      while (resAngle > Math.PI) {
         resAngle -= 2 * Math.PI;
      }
      while (resAngle < -Math.PI) {
         resAngle += 2 * Math.PI;
      }
      return resAngle;
   }

   public static double linearInterpolate(double currValue, double minValue, double maxValue,
                                          double startTarget, double endTarget) {
      double percent = (currValue - minValue) / (maxValue - minValue);
      return startTarget + (endTarget - startTarget) * percent;
   }

   public static <T, V extends Comparable<V>> T getWithMin(Stream<T> stream, Function<T, V> extractor) {
      V minValue = null;
      T res = null;
      for (T t : stream.collect(Collectors.toList())) {
         V value = extractor.apply(t);
         if (minValue == null || minValue.compareTo(value) > 0) {
            minValue = value;
            res = t;
         }
      }
      return res;
   }

   public static boolean isCapturedByMe(Facility f) {
      RectSelection rect = RectSelection.getFacilitySelection(f);
      long capturingUnits = rect.filterUnitsInSelection(Army.world.getMyUnits()).stream().filter(u ->
              !Utils.isAirUnit(u.getType())).count();
      return capturingUnits > 5;
   }

   public static boolean isCapturedByEnemy(Facility f) {
      RectSelection rect = RectSelection.getFacilitySelection(f);
      long capturingUnits = rect.filterUnitsInSelection(Army.world.getEnemyUnits()).stream().filter(u ->
              !Utils.isAirUnit(u.getType())).count();
      return capturingUnits > 5;
   }

   public static double getWeatherVisionFactor(WeatherType weatherType) {
      Game game = Army.game;
      double res = 1;
      switch (weatherType) {
         case CLEAR:
            res = game.getClearWeatherVisionFactor();
            break;
         case CLOUD:
            res = game.getCloudWeatherVisionFactor();
            break;
         case RAIN:
            res = game.getRainWeatherVisionFactor();
            break;
      }
      return res;
   }

   public static double getTerrainVisionFactor(TerrainType terrainType) {
      Game game = Army.game;
      double res = 1;
      switch (terrainType) {
         case PLAIN:
            res = game.getPlainTerrainVisionFactor();
            break;
         case SWAMP:
            res = game.getSwampTerrainVisionFactor();
            break;
         case FOREST:
            res = game.getForestTerrainVisionFactor();
            break;
      }
      return res;
   }

   public static List<Point> getPositionsOfCells(List<CellInfo> cells) {
      return cells.stream().map(CellInfo::getCenterOfCell).collect(Collectors.toList());
   }

   public static List<Point> getPositionsOfUnits(List<BattleUnit> battleUnits) {
      return battleUnits.stream().map(BattleUnit::getPos).collect(Collectors.toList());
   }

   public static List<BattleUnit> filterGroundUnits(List<BattleUnit> units) {
      return units.stream().filter(u -> !u.isAerial()).collect(Collectors.toList());
   }

   public static List<BattleUnit> filterAirUnits(List<BattleUnit> units) {
      return units.stream().filter(BattleUnit::isAerial).collect(Collectors.toList());
   }

   public static double getWeatherSpeedFactor(WeatherType weatherType) {
      Game game = Army.game;
      double res = 1;
      switch (weatherType) {
         case CLEAR:
            res = game.getClearWeatherSpeedFactor();
            break;
         case CLOUD:
            res = game.getCloudWeatherSpeedFactor();
            break;
         case RAIN:
            res = game.getRainWeatherSpeedFactor();
            break;
      }
      return res;
   }

   public static double getTerrainSpeedFactor(TerrainType terrainType) {
      Game game = Army.game;
      double res = 1;
      switch (terrainType) {
         case PLAIN:
            res = game.getPlainTerrainSpeedFactor();
            break;
         case SWAMP:
            res = game.getSwampTerrainSpeedFactor();
            break;
         case FOREST:
            res = game.getForestTerrainSpeedFactor();
            break;
      }
      return res;
   }

   public static Group getGroupWithMoreUnits(Group g1, Group g2) {
      return g1.size() > g2.size() ? g1 : g2;
   }

   public static Group getGroupWithLessUnits(Group g1, Group g2) {
      return g1.size() < g2.size() ? g1 : g2;
   }

   public static double getSpeed(VehicleType vehicleType) {
      Game game = Army.game;
      double res = 1;
      switch (vehicleType) {
         case ARRV:
            res = game.getArrvSpeed();
            break;
         case FIGHTER:
            res = game.getFighterSpeed();
            break;
         case HELICOPTER:
            res = game.getHelicopterSpeed();
            break;
         case IFV:
            res = game.getIfvSpeed();
            break;
         case TANK:
            res = game.getTankSpeed();
            break;
      }
      return res;
   }

   public static double getAttackRange(VehicleType vehicleType) {
      Game game = Army.game;
      double res = 1;
      switch (vehicleType) {
         case ARRV:
            res = game.getArrvRepairRange();
            break;
         case FIGHTER:
            res = game.getFighterAerialAttackRange();
            break;
         case HELICOPTER:
            res = game.getHelicopterGroundAttackRange();
            break;
         case IFV:
            res = game.getIfvAerialAttackRange();
            break;
         case TANK:
            res = game.getTankAerialAttackRange();
            break;
      }
      return res;
   }

   public static double getVisionRange(VehicleType vehicleType) {
      Game game = Army.game;
      double res = 1;
      switch (vehicleType) {
         case ARRV:
            res = game.getArrvVisionRange();
            break;
         case FIGHTER:
            res = game.getFighterVisionRange();
            break;
         case HELICOPTER:
            res = game.getHelicopterVisionRange();
            break;
         case IFV:
            res = game.getIfvVisionRange();
            break;
         case TANK:
            res = game.getTankVisionRange();
            break;
      }
      return res;
   }

   public static Point calcAveragePosOfUnits(List<BattleUnit> units) {
      double sumX = 0;
      double sumY = 0;
      for (BattleUnit u : units) {
         Point pos = u.getPos();
         sumX += pos.getX();
         sumY += pos.getY();
      }
      double x = sumX / units.size();
      double y = sumY / units.size();
      return new Point(x, y);
   }

   public static Point calcAveragePos(List<Point> points) {
      double sumX = 0;
      double sumY = 0;
      for (Point p : points) {
         sumX += p.getX();
         sumY += p.getY();
      }
      double x = sumX / points.size();
      double y = sumY / points.size();
      return new Point(x, y);
   }

   public static Vector2D calcAverageVelocity(List<BattleUnit> units) {
      Vector2D sum = new Vector2D(0, 0);
      if (units.size() == 0) {
         return sum;
      }

      for (BattleUnit u : units) {
         sum = sum.plus(u.getVelocity());
      }

      return sum.multiply(1.0 / units.size());
   }

   public static List<BattleUnit> filterUnitsInRange(List<BattleUnit> units, Point pos, double range) {
      return units.stream().filter(u -> u.getPos().distanceTo(pos) < range).collect(Collectors.toList());
   }

   public static double calcCircleSquare(double radius) {
      return Math.PI * radius * radius;
   }

   public static double calcArcLength(double radius, double angle) {
      return Math.abs(angle) * radius;
   }

   public static double calcRotationDuration(Group g, double angleToRotate) {
      double rotationFarUnitDist = Utils.calcArcLength(g.getGroupRadius(), angleToRotate);
      return rotationFarUnitDist / Utils.getSpeed(g.getVehicleType());
   }

   public static boolean isAirUnit(VehicleType type) {
      return type != null && type.equals(VehicleType.HELICOPTER) || type.equals(VehicleType.FIGHTER);
   }

   public static <T extends BattleUnit> T findNearest(Point p, List<T> units) {
      T res = null;
      double minDist = Integer.MAX_VALUE;
      for (T u : units) {
         double dist = p.distanceTo(u.getPos());
         if (dist < minDist) {
            res = u;
            minDist = dist;
         }
      }

      return res;
   }

   public static Point ensureInBoundsOfField(Point pos) {
      return ensureInBoundsOfField(pos, SMALL_DOUBLE);
   }

   public static Point ensureInBoundsOfField(Point pos, double distFromBorders) {
      WorldInfo world = Army.world;

      double x = pos.getX();
      double y = pos.getY();
      if (x < distFromBorders) {
         x = distFromBorders;
      }
      else if (x >= world.getWidth() - distFromBorders) {
         x = world.getWidth() - distFromBorders;
      }
      if (y < distFromBorders) {
         y = distFromBorders;
      }
      else if (y >= world.getHeight() - distFromBorders) {
         y = world.getHeight() - distFromBorders;
      }
      return new Point(x, y);
   }

   public static Point ensureInBoundsOfRotate(Point pos) {
      WorldInfo world = Army.world;

      double x = pos.getX();
      double y = pos.getY();
      if (x < -world.getWidth()) {
         x = -world.getWidth() + SMALL_DOUBLE;
      }
      else if (x > 2 * world.getWidth()) {
         x = 2 * world.getWidth() - SMALL_DOUBLE;
      }
      if (y < -world.getHeight()) {
         y = SMALL_DOUBLE;
      }
      else if (y > 2 * world.getHeight()) {
         y = 2 * world.getHeight() - SMALL_DOUBLE;
      }
      return new Point(x, y);
   }

   public static double ensureInBounds(double value, double minValue, double maxValue) {
      return Math.min(Math.max(value, minValue), maxValue);
   }

   public static List<BattleUnit> getUnitsWeakToVehicleType(List<BattleUnit> units, VehicleType vehicleType) {
      return filterUnitsByTypes(units, unitsCounterMatrix.get(vehicleType));
   }

   public static List<VehicleType> getCounterUnitTypes(VehicleType vehicleType) {
      List<VehicleType> res = new ArrayList<>();
      for (Map.Entry<VehicleType, List<VehicleType>> entry : unitsCounterMatrix.entrySet()) {
         if (entry.getValue().contains(vehicleType)) {
            res.add(entry.getKey());
         }
      }
      return res;
   }

   public static List<BattleUnit> filterUnitsByTypes(List<BattleUnit> units, List<VehicleType> types) {
      return units.stream().filter(u -> types.contains(u.getType())).collect(Collectors.toList());
   }

   public static List<BattleUnit> filterUnitsByType(List<BattleUnit> units, VehicleType type) {
      return units.stream().filter(u -> u.getType().equals(type)).collect(Collectors.toList());
   }

   public static double hypot(double x, double y) {
      return StrictMath.sqrt(x * x + y * y);
   }

   public static VehicleType getVehicleTypeWithMaxUnits(List<BattleUnit> battleUnits) {
      int[] count = new int[VehicleType.values().length];
      for (BattleUnit u : battleUnits) {
         count[u.getType().ordinal()]++;
      }

      int maxCount = 0;
      int indexOfMaxCount = 0;
      for (int i = 0; i < count.length; ++i) {
         if (count[i] > maxCount) {
            maxCount = count[i];
            indexOfMaxCount = i;
         }
      }

      return VehicleType.values()[indexOfMaxCount];
   }

   public static double calcPointsAxis(List<Point> points) {
      return calcPointsAxis(points, null, null, null);
   }

   public static double calcPointsAxis(List<Point> points, Point center, Double radius, Double startPointsAxis) {
      if (center == null) {
         center = Utils.calcAveragePos(points);
      }

      Point mostFarPoint = null;
      if (radius == null || startPointsAxis == null) {
         mostFarPoint = center.findMostFarPoint(points);
      }

      if (radius == null) {
         radius = center.distanceTo(mostFarPoint);
      }

      if (startPointsAxis == null) {
         startPointsAxis = new Vector2D(center, mostFarPoint).angle();
      }

      Vector2D startAxisVector = Vector2D.fromAngleAndLength(startPointsAxis, radius);
      Vector2D currAxisVector = startAxisVector;
      Vector2D bestAxisVector = null;

      double angleStep = Math.PI / 90;
      boolean goForward = true;

      double minSumDist = Integer.MAX_VALUE;

      while (true) {
         Segment s = new Segment(center.plus(currAxisVector), center.plus(currAxisVector.negate()));
         double sumDist = 0;
         double maxDist = 0;
         for (Point p : points) {
            float dist = s.distanceTo(p);
            if (dist > maxDist) {
               maxDist = dist;
            }
            sumDist += dist;
         }

         if (sumDist < minSumDist) {
            bestAxisVector = currAxisVector;
            minSumDist = sumDist;
            currAxisVector = currAxisVector.rotate(goForward ? angleStep : -angleStep);
         }
         else {
            if (goForward) {
               goForward = false;
               currAxisVector = startAxisVector.rotate(-angleStep);
            }
            else {
               break;
            }
         }
      }

      double axis = bestAxisVector.angle();
      if (axis < 0) {
         axis += Math.PI;
      }

      return axis;
   }

}
