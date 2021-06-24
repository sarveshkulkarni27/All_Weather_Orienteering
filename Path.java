/**
 * This program produces the optimal path from start point to 
 * destination point using A Star Algorithm for various seasons
 * 
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Path extends JFrame{

	double[][] coordinateElevationArray = new double[500][400];
	double straightDistance = 0;
	ArrayList<Integer> destinationPathCoordinates = new ArrayList<Integer>();
	ArrayList<Double> pathElevationList = new ArrayList<Double>();
	ArrayList<Integer> neighborCoordinates = new ArrayList<Integer>();
	Color[][] terrainColorArray;
	BufferedImage displayImage;
	Map<String, Integer> colorMap = new HashMap<String, Integer>();
	double baseElevation;
	List<Integer> parentCoordinates = new ArrayList<Integer>();
	Map<String, Set<Integer>> waterEdgeParent = new HashMap<String, Set<Integer>>();
	List<Integer> displayWaterPath = new ArrayList<Integer>();
	double totalPathLength = 0;
	BufferedImage mapImage;


	static int openLand = 20;
	static int roughMeadow = 12;
	static int easyMovementForest = 18;
	static int slowRunForest = 16;
	static int walkForest = 14;
	static int pavelRoad = 22;
	static int footPath = 24;


	double hypDistance = Math.sqrt((10.29*10.29) + (7.55*7.55));


	public Path() {
		setBounds(100, 100, 395, 500);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	/**
	 * Loads the Elevation File
	 * 
	 * @param 	scanner			Scanner Object to load the file
	 */
	
	public void fileLoad(Scanner scanner) {

		for(int yCoordinate = 0; yCoordinate <= 499; yCoordinate++) {
			for(int xCoordinate = 0; xCoordinate <= 394; xCoordinate++) {
				coordinateElevationArray[yCoordinate][xCoordinate] = scanner.nextDouble();
				if(baseElevation == 0 || coordinateElevationArray[yCoordinate][xCoordinate] < baseElevation) {
					baseElevation = coordinateElevationArray[yCoordinate][xCoordinate];
				}
				if(xCoordinate == 394 && scanner.hasNextLine()) {
					scanner.nextLine();
				}
			}
		}
	}

	/**
	 * Load the Image pixels
	 * 
	 * @param 		mapPath			Stores the image
	 * 
	 * @throws 		IOException		To handle if an IO exception occurs
	 */
	
	public void readMap(String mapPath) throws IOException {
		mapImage = ImageIO.read(new File(mapPath));	
		terrainColorArray = new Color[mapImage.getWidth()][mapImage.getHeight()];
		for(int yCoordinate = 0; yCoordinate < mapImage.getHeight(); yCoordinate++) {
			for(int xCoordinate = 0; xCoordinate < mapImage.getWidth(); xCoordinate++) {
				terrainColorArray[xCoordinate][yCoordinate] = new Color(mapImage.getRGB(xCoordinate, yCoordinate));
			}
		}	
	}

	/**
	 * Sets the output image
	 * 
	 * @param 		outputFileName		File to save the image
	 * 
	 * @throws 		IOException			To handle if an IO exception occurs
	 */
	
	public void displayImage(String outputFileName) throws IOException {
		displayImage = new BufferedImage(terrainColorArray.length, terrainColorArray[0].length, BufferedImage.TYPE_INT_RGB);
		for(int xCoordinate = 0; xCoordinate < terrainColorArray.length; xCoordinate++) {
			for(int yCoordinate = 0; yCoordinate < terrainColorArray[xCoordinate].length; yCoordinate++) {
				displayImage.setRGB(xCoordinate, yCoordinate, terrainColorArray[xCoordinate][yCoordinate].getRGB());
			}
		}
		File outputFile = new File(outputFileName);
		ImageIO.write(displayImage, "png", outputFile);
		repaint();
	}

	/**
	 * Displays the output image
	 * 
	 * @param		g			Display the image
	 */
	
	public void paint(Graphics g) {
		g.drawImage(displayImage, 0, 0, this);
	}

	/**
	 * Read the destination coordinates
	 * 
	 * @param 		destinationPath			Scanner object to read the destination coordinates
	 */
	
	public void readDestinationPath(Scanner destinationPath) {
		while(destinationPath.hasNext()) {
			destinationPathCoordinates.add(destinationPath.nextInt());
		}		
	}

	/**
	 * Elevation for each destination coordinates in the path
	 * 
	 */
	
	public void getElevationPerPixel() {
		for(int index = 0; index < destinationPathCoordinates.size() - 2; index+=2) {
			pathElevationList.add(coordinateElevationArray[index + 1][index]);
		}
	}

	/**
	 * Calculate straight line distance
	 * 
	 * @param 		x1			X Coordinate of start
	 * 
	 * @param 		y1			Y Coordinate of start
	 * 	
	 * @param 		x2			X Coordinate of destination
	 * 
	 * @param 		y2			Y Coordinate of destination
	 * 
	 * @param 		h1			Elevation of Start
	 * 
	 * @param 		h2			Elevation of Destination
	 * 
	 * @return					Straight Line Distance
	 * 
	 */
	
	public double calculateStraightDistance(int x1, int y1, int x2, int y2, double h1, double h2) {
		double xDistance = (x1 - x2) * 10.29;
		double yDistance = (y1 - y2) * 7.55;
		double hDistance = h1 - h2;

		straightDistance = Math.sqrt(xDistance*xDistance + yDistance*yDistance + hDistance*hDistance);
		return straightDistance;
	}

	/**
	 * Get the neighboring coordinates
	 * 
	 * @param 		currentX		X Coordinate of Current
	 * 
	 * @param 		currentY		Y Coordinate of Current
	 * 
	 * @return						Neighboring Coordinates of Current
	 * 
	 */
	
	public ArrayList<Integer> getNeighborCoordinates(int currentX, int currentY) {
		int calculateNeighborCoordinates[][] = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for(int index = 0; index < 4; index++) {
			int neighborX = currentX + calculateNeighborCoordinates[index][0];
			int neighborY = currentY + calculateNeighborCoordinates[index][1];
			if((0 <= neighborX && neighborX <= 394) && (0 <= neighborY && neighborY <= 499)) {
				neighborCoordinates.add(neighborX);
				neighborCoordinates.add(neighborY);
			}
		}
		return neighborCoordinates;
	}

	/**
	 * Get the neighboring coordinates
	 * 
	 * @param 		currentX		X Coordinate of Current
	 * 
	 * @param 		currentY		Y Coordinate of Current
	 * 
	 * @return						Neighboring Coordinates of Current
	 */
	
	public ArrayList<Integer> getPixelNeighbor(int currentX, int currentY) {
		int calculateNeighborCoordinates[][] = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
		for(int index = 0; index < 8; index++) {
			int neighborX = currentX + calculateNeighborCoordinates[index][0];
			int neighborY = currentY + calculateNeighborCoordinates[index][1];
			if((0 <= neighborX && neighborX <= 394) && (0 <= neighborY && neighborY <= 499)) {
				neighborCoordinates.add(neighborX);
				neighborCoordinates.add(neighborY);
			}
		}
		return neighborCoordinates;
	}

	/**
	 * Store the total path
	 * 
	 * @param 		weather       Current Weather
	 * 
	 */
	
	public void calculatePath(String weather) {
		List<Integer> displayFinalPath = new ArrayList<Integer>();
		for(int index = 0; index < destinationPathCoordinates.size() - 3; index+=2) {
			displayFinalPath.addAll(aStarSearch(destinationPathCoordinates.get(index), destinationPathCoordinates.get(index + 1), 
					destinationPathCoordinates.get(index + 2), destinationPathCoordinates.get(index + 3), weather));
		}
		terrainColorArray[destinationPathCoordinates.get(0)][destinationPathCoordinates.get(1)] = new Color(255, 0, 0);
		for( int indexColor = 0; indexColor < displayFinalPath.size() - 1; indexColor+=2) {
			terrainColorArray[displayFinalPath.get(indexColor)][displayFinalPath.get(indexColor + 1)] = new Color(255, 0, 0);
		}
		
		List<Integer> pixelHighhlightList = new ArrayList<Integer>();
		for(int index = 0; index < destinationPathCoordinates.size() - 1; index += 2) {
			pixelHighhlightList = getPixelNeighbor(destinationPathCoordinates.get(index), destinationPathCoordinates.get(index + 1));

			for( int indexColor = 0; indexColor < pixelHighhlightList.size() - 1; indexColor+=2) {
				int xNeighborValue = pixelHighhlightList.get(indexColor);
				int yNeighborValue = pixelHighhlightList.get(indexColor + 1);

				terrainColorArray[xNeighborValue][yNeighborValue] = new Color(75, 0, 130);
			}
		}

	}

	/**
	 * Default speed for various paths according to the weather
	 * 
	 * @param 		weather			Current Weather	
	 */
	
	public void addDefaultSpeed(String weather) {
		colorMap.put("248,148,18", openLand);
		colorMap.put("255,192,0", roughMeadow);
		colorMap.put("255,255,255", easyMovementForest);
		colorMap.put("2,208,60", slowRunForest);
		colorMap.put("2,136,40", walkForest);
		colorMap.put("71,51,3", pavelRoad);
		colorMap.put("0,0,0", footPath);
		if(weather.equals("winter")) {
			colorMap.put("135,206,250", 14);
		}
		if(weather.equals("spring")) {
			colorMap.put("139,69,19", 6);
		}

	}

	/**
	 * Calculate time to travel between two coordinates according to the color of pixels
	 * 
	 * @param 		currentX			X Coordinate of the current
	 * 
	 * @param 		currentY			Y Coordinate of the current
	 * 
	 * @param 		xNeighborValue		X Coordinate of the Neighbor
	 * 
	 * @param 		yNeighborValue		Y Coordinate of the Neighbor
	 * 
	 * @param 		index				Direction of the Neighbor with respect to current coordinate
	 * 		
	 * @param 		weather				Current Weather
	 * 
	 * @return							Time from current coordinate to destination coordinate
	 */
	
	public double getColorTime(int currentX, int currentY, int xNeighborValue, int yNeighborValue, int index, String weather, Map<String, Double> gValue) {
		
		String previousGCoordinate = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
		double previousGValue = gValue.get(previousGCoordinate);
		
		String neighborGCoordinate = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);

		double gDistance = 0;		
		int currentColor = terrainColorArray[currentX][currentY].getRGB();
		int neighborColor = terrainColorArray[xNeighborValue][yNeighborValue].getRGB();

		int redCurrent = (currentColor >> 16) & 0xff;
		int greenCurrent = (currentColor >> 8) & 0xff;
		int blueCurrent = (currentColor) & 0xff;

		String finalCurrentColor = Integer.valueOf(redCurrent) + "," + Integer.valueOf(greenCurrent) + "," + Integer.valueOf(blueCurrent);

		int redNeighbor = (neighborColor >> 16) & 0xff;
		int greenNeighbor = (neighborColor >> 8) & 0xff;
		int blueNeighbor = (neighborColor) & 0xff;

		String finalNeighborColor = Integer.valueOf(redNeighbor) + "," + Integer.valueOf(greenNeighbor) + "," + Integer.valueOf(blueNeighbor);

		double nextElevation = coordinateElevationArray[yNeighborValue][xNeighborValue];
		double currentElevation = coordinateElevationArray[currentY][currentX];

		gDistance = calculateStraightDistance(currentX, currentY, xNeighborValue, yNeighborValue, currentElevation, nextElevation);
		double time = 0;

		if(weather.equals("fall") && finalCurrentColor.equals("255,255,255")) {
			colorMap.put("248,148,18", openLand - 2);
			colorMap.put("255,192,0", roughMeadow - 1);
			colorMap.put("255,255,255", easyMovementForest - 8);
			colorMap.put("2,208,60", slowRunForest - 2);
			colorMap.put("2,136,40", walkForest - 4);
			colorMap.put("71,51,3", pavelRoad - 1);
			colorMap.put("0,0,0", footPath - 4);
		}
		
		if(index == 0 || index == 2) {
			time = previousGValue + ((gDistance / ((10.29/2) * colorMap.get(finalCurrentColor))) + (gDistance / ((10.29/2) * colorMap.get(finalNeighborColor))));
		}
		if(index == 4 || index == 6) {
			time = previousGValue + ((gDistance / ((7.55/2) * colorMap.get(finalCurrentColor))) + (gDistance / ((7.55/2) * colorMap.get(finalNeighborColor))));
		}else {
			time = previousGValue + ((gDistance / ((hypDistance) * colorMap.get(finalCurrentColor))) + (gDistance / ((hypDistance) * colorMap.get(finalNeighborColor))));
		}			
		
		
		if(gValue.containsKey(neighborGCoordinate)) {	
			double previousNeighborGValue = gValue.get(neighborGCoordinate);
			if(time < previousNeighborGValue) {
				gValue.put(neighborGCoordinate, time);
			}
			else {
				time = previousNeighborGValue;
			}
		}else {
			gValue.put(neighborGCoordinate, time);
		}

		if(weather.equals("fall") && finalCurrentColor.equals("255,255,255")) {
			addDefaultSpeed(weather);
		}

		return time;
	}

	/**
	 * Calculate the path between start and destination
	 * 
	 * @param 		parentCoordinate					Path Coordinates
	 * 
	 * @param 		parentCoordinatesValue				Path Coordinates to search
	 * 
	 * @param 		keySet								Coordinates
	 */
	
	public void getPath(Map<String, Set<String>> parentCoordinate, String parentCoordinatesValue, Set<String> keySet) {
		for(Set<String> mapValues : parentCoordinate.values()) {
			if(mapValues.contains(parentCoordinatesValue)) {
				for(String key : keySet) {
					if(parentCoordinate.get(key).contains(parentCoordinatesValue)) {
						String[] parentXYValues = parentCoordinatesValue.split(",");
						int parentXValue = Integer.parseInt(parentXYValues[0]);
						int parentYValue = Integer.parseInt(parentXYValues[1]);
						parentCoordinates.add(parentXValue);
						parentCoordinates.add(parentYValue);
						parentCoordinate.get(key).remove(parentCoordinatesValue);
						parentCoordinatesValue = key;
						getPath(parentCoordinate, parentCoordinatesValue, keySet);
					}
				}
			}

		}
	}

	/**
	 * Calculate the distance between start and end coordinates according to the weather
	 * 
	 * @param 		x1				X Coordinate of Start
	 * 
	 * @param 		y1				Y Coordinate of Start
	 * 
	 * @param 		x2				X Coordinate of Destination
	 * 
	 * @param 		y2				Y Coordinate of Destination
	 * 
	 * @param 		weather			Current Weather
	 * 
	 * @return						Visited Path Coordinates
	 */
	
	public List<Integer> aStarSearch(int x1, int y1, int x2, int y2, String weather) {

		Map<String, Double> updateNodeMap = new HashMap<String, Double>();
		Map<String, Set<String>> parentCoordinate = new HashMap<String, Set<String>>();

		PriorityQueue<Double> fPriority = new PriorityQueue<Double>();
		Map<Integer, Set<Integer>> openCoordinates = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> closeCoordinates = new HashMap<Integer, Set<Integer>>();
		
		Map<String, Double> gValue = new HashMap<String, Double>();
		String gCoordinate = Integer.valueOf(x1) + "," + Integer.valueOf(y1);
		gValue.put(gCoordinate, 0.0);

		int currentX = x1;
		int currentY = y1;
		int destinationX = x2;
		int destinationY = y2;
		int xNeighborValue = 0;
		int yNeighborValue = 0;
		int lowestFXCoordinate = 0;
		int lowestFYCoordinate = 0;
		double gDistance = 0;
		double newFValue= 0;
		String coordinates = "";
		String parentCoordinatesValue = "";
		double nextElevation = coordinateElevationArray[yNeighborValue][xNeighborValue];
		double currentElevation = coordinateElevationArray[currentY][currentX];
		double destinationElevation = coordinateElevationArray[destinationY][destinationX];
		double heuristicDistance = calculateStraightDistance(x1, y1, x2, y2, currentElevation, nextElevation);
		double fValue = heuristicDistance;

		while(!(currentX == destinationX && currentY == destinationY)) {

			ArrayList<Integer> requiredNeighborCoordinates = getNeighborCoordinates(currentX, currentY);
			for(int index = 0; index < requiredNeighborCoordinates.size(); index+=2) {
				xNeighborValue = requiredNeighborCoordinates.get(index);
				yNeighborValue = requiredNeighborCoordinates.get(index + 1);

				int neighborColor = terrainColorArray[xNeighborValue][yNeighborValue].getRGB();

				int redNeighbor = (neighborColor >> 16) & 0xff;
				int greenNeighbor = (neighborColor >> 8) & 0xff;
				int blueNeighbor = (neighborColor) & 0xff;

				String finalNeighborColor = Integer.valueOf(redNeighbor) + "," + Integer.valueOf(greenNeighbor) + "," + Integer.valueOf(blueNeighbor);

				if(colorMap.containsKey(finalNeighborColor)) {
					gDistance = getColorTime(currentX, currentY, xNeighborValue, yNeighborValue, index, weather, gValue);
					heuristicDistance = calculateStraightDistance(xNeighborValue, yNeighborValue, destinationX, destinationY, nextElevation, destinationElevation) / openLand;

					newFValue = gDistance + heuristicDistance;

					if(!openCoordinates.containsKey(xNeighborValue) && !closeCoordinates.containsKey(xNeighborValue) ) {

						Set<Integer> mapOCoordinateInteger = new HashSet<Integer>();
						mapOCoordinateInteger.add(yNeighborValue);
						openCoordinates.put(xNeighborValue, mapOCoordinateInteger);

						fPriority.add(newFValue);

						coordinates = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);

						updateNodeMap.put(coordinates, newFValue);
						parentCoordinatesValue = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
						if(!parentCoordinate.containsKey(parentCoordinatesValue)) {
							Set<String> pCoordinateVal = new HashSet<String>();
							pCoordinateVal.add(coordinates);
							parentCoordinate.put(parentCoordinatesValue, pCoordinateVal);
						}else {
							parentCoordinate.get(parentCoordinatesValue).add(coordinates);
						}

					}
					if(!closeCoordinates.containsKey(xNeighborValue) 
							&& (openCoordinates.containsKey(xNeighborValue) && !openCoordinates.get(xNeighborValue).contains(yNeighborValue))) {
						openCoordinates.get(xNeighborValue).add(yNeighborValue);
						fPriority.add(newFValue);

						coordinates = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);
						updateNodeMap.put(coordinates, newFValue);
						parentCoordinatesValue = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
						if(!parentCoordinate.containsKey(parentCoordinatesValue)) {
							Set<String> pCoordinateVal = new HashSet<String>();
							pCoordinateVal.add(coordinates);
							parentCoordinate.put(parentCoordinatesValue, pCoordinateVal);
						}else {
							parentCoordinate.get(parentCoordinatesValue).add(coordinates);
						}
					}
					if(!openCoordinates.containsKey(xNeighborValue) 
							&& (closeCoordinates.containsKey(xNeighborValue) && !closeCoordinates.get(xNeighborValue).contains(yNeighborValue))) {
						Set<Integer> mapOCoordinateInteger = new HashSet<Integer>();
						mapOCoordinateInteger.add(yNeighborValue);
						openCoordinates.put(xNeighborValue, mapOCoordinateInteger);

						fPriority.add(newFValue);

						coordinates = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);
						updateNodeMap.put(coordinates, newFValue);
						parentCoordinatesValue = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
						if(!parentCoordinate.containsKey(parentCoordinatesValue)) {
							Set<String> pCoordinateVal = new HashSet<String>();
							pCoordinateVal.add(coordinates);
							parentCoordinate.put(parentCoordinatesValue, pCoordinateVal);
						}else {
							parentCoordinate.get(parentCoordinatesValue).add(coordinates);
						}
					}
					if((openCoordinates.containsKey(xNeighborValue) && !openCoordinates.get(xNeighborValue).contains(yNeighborValue)) 
							&& (closeCoordinates.containsKey(xNeighborValue) && !closeCoordinates.get(xNeighborValue).contains(yNeighborValue))) {
						openCoordinates.get(xNeighborValue).add(yNeighborValue);

						fPriority.add(newFValue);

						coordinates = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);
						updateNodeMap.put(coordinates, newFValue);
						parentCoordinatesValue = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
						if(!parentCoordinate.containsKey(parentCoordinatesValue)) {
							Set<String> pCoordinateVal = new HashSet<String>();
							pCoordinateVal.add(coordinates);
							parentCoordinate.put(parentCoordinatesValue, pCoordinateVal);
						}else {
							parentCoordinate.get(parentCoordinatesValue).add(coordinates);
						}
					}


					if((!closeCoordinates.containsKey(xNeighborValue))
							&& openCoordinates.containsKey(xNeighborValue) && openCoordinates.get(xNeighborValue).contains(yNeighborValue)) {
						coordinates = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);
						parentCoordinatesValue = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
						double oldFValue = updateNodeMap.get(coordinates);
						if(newFValue < oldFValue) {
							fPriority.remove(oldFValue);
							fPriority.add(newFValue);

							updateNodeMap.remove(coordinates);
							updateNodeMap.put(coordinates, newFValue);
							Set<String> keySet = parentCoordinate.keySet();
							for(String key : keySet) {
								if(parentCoordinate.get(key).contains(coordinates)) {
									parentCoordinate.get(key).remove(coordinates);
									if(!parentCoordinate.containsKey(parentCoordinatesValue)) {
										Set<String> pCoordinateVal = new HashSet<String>();
										pCoordinateVal.add(coordinates);
										parentCoordinate.put(parentCoordinatesValue, pCoordinateVal);
									}else {
										parentCoordinate.get(parentCoordinatesValue).add(coordinates);
									}
									break;
								}
							}
						}
					}

					if((closeCoordinates.containsKey(xNeighborValue) && !closeCoordinates.get(xNeighborValue).contains(yNeighborValue))
							&& openCoordinates.containsKey(xNeighborValue) && openCoordinates.get(xNeighborValue).contains(yNeighborValue)) {
						coordinates = Integer.valueOf(xNeighborValue) + "," + Integer.valueOf(yNeighborValue);
						parentCoordinatesValue = Integer.valueOf(currentX) + "," + Integer.valueOf(currentY);
						double oldFValue = updateNodeMap.get(coordinates);

						if(newFValue < oldFValue) {
							fPriority.remove(oldFValue);
							fPriority.add(newFValue);

							updateNodeMap.remove(coordinates);
							updateNodeMap.put(coordinates, newFValue);
							Set<String> keySet = parentCoordinate.keySet();
							for(String key : keySet) {
								if(parentCoordinate.get(key).contains(coordinates)) {
									parentCoordinate.get(key).remove(coordinates);
									if(!parentCoordinate.containsKey(parentCoordinatesValue)) {
										Set<String> pCoordinateVal = new HashSet<String>();
										pCoordinateVal.add(coordinates);
										parentCoordinate.put(parentCoordinatesValue, pCoordinateVal);
									}else {
										parentCoordinate.get(parentCoordinatesValue).add(coordinates);
									}
									break;
								}
							}
						}
					}
				}
				if(index == requiredNeighborCoordinates.size() - 2 || (xNeighborValue == destinationX && yNeighborValue == destinationY)) {
					if(fPriority.size() == 0) {
						System.out.println();
					}

					if(xNeighborValue == destinationX && yNeighborValue == destinationY) {

						Set<String> keys = parentCoordinate.keySet();
						String[] neighborXYValues = coordinates.split(",");
						int neighborXValue = Integer.parseInt(neighborXYValues[0]);
						int neighborYValue = Integer.parseInt(neighborXYValues[1]);

						String[] parentXYValues = parentCoordinatesValue.split(",");
						int parentXValue = Integer.parseInt(parentXYValues[0]);
						int parentYValue = Integer.parseInt(parentXYValues[1]);
						parentCoordinates.add(neighborXValue);
						parentCoordinates.add(neighborYValue);
						parentCoordinates.add(parentXValue);
						parentCoordinates.add(parentYValue);

						lowestFXCoordinate = xNeighborValue;
						lowestFYCoordinate = yNeighborValue;


						getPath(parentCoordinate, parentCoordinatesValue, keys);
					}else {

						double lowestFValue = fPriority.poll();
						Set<String> keySet = updateNodeMap.keySet();
						String nextKey = "";

						for( String keys : keySet ) {
							if(updateNodeMap.get(keys).equals(lowestFValue)) {
								nextKey = keys;
								updateNodeMap.remove(keys);
								break;
							}
						}

						String[] xyValues = nextKey.split(",");
						lowestFXCoordinate = Integer.parseInt(xyValues[0]);
						lowestFYCoordinate = Integer.parseInt(xyValues[1]);

					}

					requiredNeighborCoordinates.clear();

				}

			}

			if(!closeCoordinates.containsKey(currentX)) {
				Set<Integer> mapCCoordinateInteger = new HashSet<Integer>();
				mapCCoordinateInteger.add(currentY);
				closeCoordinates.put(currentX, mapCCoordinateInteger);
			}
			else {
				closeCoordinates.get(currentX).add(currentY);
			}
			currentX = lowestFXCoordinate;
			currentY = lowestFYCoordinate;
			openCoordinates.get(lowestFXCoordinate).remove(lowestFYCoordinate);
		}

		return parentCoordinates;
	}

	/**
	 * Get water Edges
	 * 
	 */
	
	public void getWaterEdges() {

		int xNeighborValue = 0;
		int yNeighborValue = 0;

		List<String> checkWater = new ArrayList<String>();
		for(int yLevel = 0; yLevel < mapImage.getHeight(); yLevel++) {
			for(int xLevel = 0; xLevel < mapImage.getWidth(); xLevel++) {
				int neighbor = terrainColorArray[xLevel][yLevel].getRGB();

				int red = (neighbor >> 16) & 0xff;
				int green = (neighbor >> 8) & 0xff;
				int blue = (neighbor) & 0xff;

				String finalColor = Integer.valueOf(red) + "," + Integer.valueOf(green) + "," + Integer.valueOf(blue);

				ArrayList<Integer> requiredNeighborCoordinates = getNeighborCoordinates(xLevel, yLevel);
				for(int index = 0; index < requiredNeighborCoordinates.size(); index+=2) {
					xNeighborValue = requiredNeighborCoordinates.get(index);
					yNeighborValue = requiredNeighborCoordinates.get(index + 1);

					int neighborColor = terrainColorArray[xNeighborValue][yNeighborValue].getRGB();

					int redCurrent = (neighborColor >> 16) & 0xff;
					int greenCurrent = (neighborColor >> 8) & 0xff;
					int blueCurrent = (neighborColor) & 0xff;

					String finalCurrentColor = Integer.valueOf(redCurrent) + "," + Integer.valueOf(greenCurrent) + "," + Integer.valueOf(blueCurrent);
					checkWater.add(finalCurrentColor);				
				}
				if(checkWater.contains("0,0,255") && !finalColor.equals("0,0,255")) {
					String xParent = Integer.toString(xLevel);
					String yParent = Integer.toString(yLevel);
					String xyParent = xParent + "," + yParent;
					Set<Integer> mapCCoordinateInteger = new HashSet<Integer>();
					mapCCoordinateInteger.add(0);
					waterEdgeParent.put(xyParent, mapCCoordinateInteger);
				}
				requiredNeighborCoordinates.clear();
				checkWater.clear();
			}
		}	

	}

	/**
	 * Display the Mud in Spring Season
	 * 
	 */
	
	public void getSpringBFS(){
		Queue<String> mudEdgeQueue = new LinkedList<String>();
		String currentCoordinate = "";
		int currentX = 0;
		int currentY = 0;
		int currentXNeighbor = 0;
		int currentYNeighbor = 0;
		Set<String> closedSet = new HashSet<String>();
		Set<String> neighborSet = new HashSet<String>();
		double baseHeight = 0;

		List<String> iterateCoordinates = new ArrayList<String>();
		List<Integer> testColor = new ArrayList<Integer>();

		for( String keys : waterEdgeParent.keySet() ) {
			mudEdgeQueue.add(keys);
		}

		for(int parentIndex = 0; parentIndex < 15; parentIndex++ ) {
			while(mudEdgeQueue.size() > 0) {
				iterateCoordinates.add(mudEdgeQueue.poll());
			}
			for(int levelIndex = 0; levelIndex < iterateCoordinates.size(); levelIndex++) {
				currentCoordinate = iterateCoordinates.get(levelIndex);
				String[] currentXY = currentCoordinate.split(",");
				currentX = Integer.parseInt(currentXY[0]);
				currentY = Integer.parseInt(currentXY[1]);
				baseHeight = coordinateElevationArray[currentY][currentX];
		
				terrainColorArray[currentX][currentY] = new Color(139, 69, 19);

				ArrayList<Integer> requiredNeighborCoordinates = getNeighborCoordinates(currentX, currentY);

				for(int index = 0; index < requiredNeighborCoordinates.size(); index+=2) {
					currentXNeighbor = requiredNeighborCoordinates.get(index);
					currentYNeighbor = requiredNeighborCoordinates.get(index + 1);

					String xNeighbor = Integer.toString(currentXNeighbor);
					String yNeighbor = Integer.toString(currentYNeighbor);
					String xyNeighbor = xNeighbor + "," + yNeighbor;

					int neighborColor = terrainColorArray[currentXNeighbor][currentYNeighbor].getRGB();
					int redCurrent = (neighborColor >> 16) & 0xff;
					int greenCurrent = (neighborColor >> 8) & 0xff;
					int blueCurrent = (neighborColor) & 0xff;
					String finalCurrentColor = Integer.valueOf(redCurrent) + "," + Integer.valueOf(greenCurrent) + "," + Integer.valueOf(blueCurrent);

					double currentHeight = Math.floor(Math.abs(coordinateElevationArray[currentYNeighbor][currentXNeighbor] - baseHeight));

					if(!closedSet.contains(xyNeighbor) 
							&& !neighborSet.contains(xyNeighbor) 
							&& !finalCurrentColor.equals("205,0,101") 
							&& !finalCurrentColor.equals("0,0,255") 
							&& currentHeight < 1
							) {
						mudEdgeQueue.add(xyNeighbor);
						neighborSet.add(xyNeighbor);
						testColor.add(currentX);
						testColor.add(currentY);
						terrainColorArray[currentXNeighbor][currentYNeighbor] = new Color(139, 69, 19);

					}
				}
				closedSet.add(currentCoordinate);
				requiredNeighborCoordinates.clear();

			}
			iterateCoordinates.clear();

		}

	}

	/**
	 * Display snow in winter season
	 * 
	 */
	
	public void getWinterBFS() {
		Queue<String> mudEdgeQueue = new LinkedList<String>();
		String currentCoordinate = "";
		int currentX = 0;
		int currentY = 0;
		int currentXNeighbor = 0;
		int currentYNeighbor = 0;
		Set<String> closedSet = new HashSet<String>();
		Set<String> neighborSet = new HashSet<String>();
		List<String> iterateCoordinates = new ArrayList<String>();
		List<Integer> testColor = new ArrayList<Integer>();

		for( String keys : waterEdgeParent.keySet() ) {
			mudEdgeQueue.add(keys);
		}

		for(int parentIndex = 0; parentIndex < 7; parentIndex++ ) {
			while(mudEdgeQueue.size() > 0) {
				iterateCoordinates.add(mudEdgeQueue.poll());
			}
			for(int levelIndex = 0; levelIndex < iterateCoordinates.size(); levelIndex++) {
				currentCoordinate = iterateCoordinates.get(levelIndex);
				String[] currentXY = currentCoordinate.split(",");
				currentX = Integer.parseInt(currentXY[0]);
				currentY = Integer.parseInt(currentXY[1]);

				testColor.add(currentX);
				testColor.add(currentY);

				ArrayList<Integer> requiredNeighborCoordinates = getNeighborCoordinates(currentX, currentY);

				for(int index = 0; index < requiredNeighborCoordinates.size(); index+=2) {
					//System.out.println(i++);
					currentXNeighbor = requiredNeighborCoordinates.get(index);
					currentYNeighbor = requiredNeighborCoordinates.get(index + 1);

					String xNeighbor = Integer.toString(currentXNeighbor);
					String yNeighbor = Integer.toString(currentYNeighbor);
					String xyNeighbor = xNeighbor + "," + yNeighbor;

					int neighborColor = terrainColorArray[currentXNeighbor][currentYNeighbor].getRGB();
					int redCurrent = (neighborColor >> 16) & 0xff;
					int greenCurrent = (neighborColor >> 8) & 0xff;
					int blueCurrent = (neighborColor) & 0xff;
					String finalCurrentColor = Integer.valueOf(redCurrent) + "," + Integer.valueOf(greenCurrent) + "," + Integer.valueOf(blueCurrent);

					if(!closedSet.contains(xyNeighbor) && !neighborSet.contains(xyNeighbor) 
							&& !mudEdgeQueue.contains(xyNeighbor) 
							&& !finalCurrentColor.equals("205,0,101") 
							&& finalCurrentColor.equals("0,0,255")
							&& !finalCurrentColor.equals("0,0,0")
							) {
						mudEdgeQueue.add(xyNeighbor);
						neighborSet.add(xyNeighbor);
						testColor.add(currentX);
						testColor.add(currentY);
						terrainColorArray[currentXNeighbor][currentYNeighbor] = new Color(135, 206, 250);
					}
				}
				closedSet.add(currentCoordinate);
				requiredNeighborCoordinates.clear();

			}
			iterateCoordinates.clear();

		}

	}
	
	/**
	 * Get the Total Path Length
	 * 
	 */
	
	public void getPathLength() {
		int x1 = 0;
		int y1 = 0;
		int x2 = 0;
		int y2 = 0;
		double currentElevation = 0;
		double destinationElevation = 0;

		parentCoordinates.add(destinationPathCoordinates.get(0));
		parentCoordinates.add(destinationPathCoordinates.get(1));


		for(int index = 0; index < parentCoordinates.size() - 3; index+=2) {
			x1 = parentCoordinates.get(index);
			y1 = parentCoordinates.get(index + 1);
			x2 = parentCoordinates.get(index + 2);
			y2 = parentCoordinates.get(index + 3);
			currentElevation = coordinateElevationArray[y1][x1];
			destinationElevation = coordinateElevationArray[y2][x2];
			totalPathLength += calculateStraightDistance(x1, y1, x2, y2, currentElevation, destinationElevation);
		}
	}

	public static void main(String[] args) throws IOException {
		if(args.length != 5) {
			System.out.println("Please add 5 arguments");
			System.exit(0);
		}
		Path shortestPath = new Path();
		shortestPath.setVisible(true);
		Scanner scanner = new Scanner(new FileInputStream(args[1]));
		shortestPath.fileLoad(scanner);
		if(args[3].equals("summer") || args[3].equals("fall") || args[3].equals("spring") || args[3].equals("winter")) {
			String weather = args[3];
			String outputFileName = args[4];
			String mapPath = args[0];
			shortestPath.readMap(mapPath);
			scanner = new Scanner(new FileInputStream(args[2]));
			shortestPath.readDestinationPath(scanner);
			shortestPath.addDefaultSpeed(weather);
			shortestPath.getElevationPerPixel();
			if(weather.equals("spring")){
				shortestPath.getWaterEdges();
				shortestPath.getSpringBFS();
			}
			if(weather.equals("winter")){
				shortestPath.getWaterEdges();
				shortestPath.getWinterBFS();
			}
			shortestPath.calculatePath(weather);
			shortestPath.displayImage(outputFileName);
			shortestPath.getPathLength();
			System.out.println("Total Path Length: " + Math.round(shortestPath.totalPathLength) + "m");
		}else {
			System.out.println("Enter Correct Season");
		}


	}

}
