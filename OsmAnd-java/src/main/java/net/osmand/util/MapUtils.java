package net.osmand.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.util.GeoPointParserUtil.GeoParsedPoint;


/**
 * This utility class includes :
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 */
public class MapUtils {

	public static final double MIN_LATITUDE = -85.0511;
	public static final double MAX_LATITUDE = 85.0511;
	public static final double LATITUDE_TURN = 180.0;
	public static final double MIN_LONGITUDE = -180.0;
	public static final double MAX_LONGITUDE = 180.0;
	public static final double LONGITUDE_TURN = 360.0;

	// TODO change the hostname back to osm.org once HTTPS works for it
	// https://github.com/openstreetmap/operations/issues/2
	private static final String BASE_SHORT_OSM_URL = "https://openstreetmap.org/go/";

	/**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified
     * in Table 1 of RFC 2045.
     */
    private static final char intToBase64[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '~'
    };

	public static double getDistance(LatLon l, double latitude, double longitude) {
		return getDistance(l.getLatitude(), l.getLongitude(), latitude, longitude);
	}

	private static double scalarMultiplication(double xA, double yA, double xB, double yB, double xC, double yC) {
		// Scalar multiplication between (AB, AC)
		return (xB - xA) * (xC - xA) + (yB - yA) * (yC - yA);
	}

	public static double getOrthogonalDistance(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
		return getDistance(getProjection(lat, lon, fromLat, fromLon, toLat, toLon), lat, lon);
	}

	public static LatLon getProjection(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
		// not very accurate computation on sphere but for distances < 1000m it is ok
		double mDist = (fromLat - toLat) * (fromLat - toLat) + (fromLon - toLon) * (fromLon - toLon);
		double projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon);
		double prlat;
		double prlon;
		if (projection < 0) {
			prlat = fromLat;
			prlon = fromLon;
		} else if (projection >= mDist) {
			prlat = toLat;
			prlon = toLon;
		} else {
			prlat = fromLat + (toLat - fromLat) * (projection / mDist);
			prlon = fromLon + (toLon - fromLon) * (projection / mDist);
		}
		return new LatLon(prlat, prlon);
	}

	public static double getProjectionCoeff(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
		// not very accurate computation on sphere but for distances < 1000m it is ok
		double mDist = (fromLat - toLat) * (fromLat - toLat) + (fromLon - toLon) * (fromLon - toLon);
		double projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon);
		double prlat;
		double prlon;
		if (projection < 0) {
			return 0;
		} else if (projection >= mDist) {
			return 1;
		} else {
			return (projection / mDist);
		}
	}

	private static double toRadians(double angdeg) {
//		return Math.toRadians(angdeg);
		return angdeg / 180.0 * Math.PI;
	}

	/**
	 * Gets distance in meters
	 */
	public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double R = 6372.8; // for haversine use R = 6372.8 km instead of 6371 km
		double dLat = toRadians(lat2 - lat1);
		double dLon = toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
		        Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
		        Math.sin(dLon / 2) * Math.sin(dLon / 2);
		//double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		//return R * c * 1000;
		// simplyfy haversine:
		return (2 * R * 1000 * Math.asin(Math.sqrt(a)));
	}


	
	/**
	 * Gets distance in meters
	 */
	public static double getDistance(LatLon l1, LatLon l2) {
		return getDistance(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude());
	}

	public static double checkLongitude(double longitude) {
		if (longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE) {
			return longitude;
		}
		while (longitude <= MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
			if (longitude < 0) {
				longitude += LONGITUDE_TURN;
			} else {
				longitude -= LONGITUDE_TURN;
			}
		}
		return longitude;
	}

	public static double checkLatitude(double latitude) {
		if (latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE) {
			return latitude;
		}
		while (latitude < -90 || latitude > 90) {
			if (latitude < 0) {
				latitude += LATITUDE_TURN;
			} else {
				latitude -= LATITUDE_TURN;
			}
		}
		if (latitude < MIN_LATITUDE) {
			return MIN_LATITUDE;
		} else if (latitude > MAX_LATITUDE) {
			return MAX_LATITUDE;
		}
		return latitude;
	}

	public static int get31TileNumberX(double longitude) {
		longitude = checkLongitude(longitude);
		long l = 1L << 31;
		return (int) ((longitude + 180d)/360d * l);
	}

	public static int get31TileNumberY(double latitude) {
		latitude = checkLatitude(latitude);
		double eval = Math.log(Math.tan(toRadians(latitude)) + 1/Math.cos(toRadians(latitude)));
		long l = 1L << 31;
		if (eval > Math.PI) {
			eval = Math.PI;
		}
		return (int) ((1 - eval / Math.PI) / 2 * l);
	}

	public static double get31LongitudeX(int tileX) {
		return MapUtils.getLongitudeFromTile(21, tileX / 1024f);
	}

	public static double get31LatitudeY(int tileY) {
		return MapUtils.getLatitudeFromTile(21, tileY / 1024f);
	}


	/**
	 * Theses methods operate with degrees (evaluating tiles & vice versa)
	 * degree longitude measurements (-180, 180) [27.56 Minsk]
	 * // degree latitude measurements (90, -90) [53.9]
	 */

	public static double getTileNumberX(float zoom, double longitude) {
		longitude = checkLongitude(longitude);
		final double powZoom = getPowZoom(zoom);
		double dz = (longitude + 180d)/360d * powZoom;
		if (dz >= powZoom) {
			return powZoom - 0.01;
		}
		return dz;
	}

	public static double getTileNumberY(float zoom, double latitude) {
		latitude = checkLatitude(latitude);
		double eval = Math.log(Math.tan(toRadians(latitude)) + 1/Math.cos(toRadians(latitude)));
		if (Double.isInfinite(eval) || Double.isNaN(eval)) {
			latitude = latitude < 0 ? -89.9 : 89.9;
			eval = Math.log(Math.tan(toRadians(latitude)) + 1/Math.cos(toRadians(latitude)));
		}
		return (1 - eval / Math.PI) / 2 * getPowZoom(zoom);
	}

	public static double getTileEllipsoidNumberY(float zoom, double latitude) {
		final double E2 = (double) latitude * Math.PI / 180;
		final long sradiusa = 6378137;
		final long sradiusb = 6356752;
		final double J2 = (double) Math.sqrt(sradiusa * sradiusa - sradiusb * sradiusb) / sradiusa;
		final double M2 = (double) Math.log((1 + Math.sin(E2))
				/ (1 - Math.sin(E2))) / 2 - J2 * Math.log((1 + J2 * Math.sin(E2)) / (1 - J2 * Math.sin(E2))) / 2;
		final double B2 = getPowZoom(zoom);
		return B2 / 2 - M2 * B2 / 2 / Math.PI;
	}

	public static double getLatitudeFromEllipsoidTileY(float zoom, float tileNumberY) {
		final double MerkElipsK = 0.0000001;
		final long sradiusa = 6378137;
		final long sradiusb = 6356752;
		final double FExct = (double) Math.sqrt(sradiusa * sradiusa
				- sradiusb * sradiusb)
				/ sradiusa;
		final double TilesAtZoom = getPowZoom(zoom);
		double result = (tileNumberY - TilesAtZoom / 2)
				/ -(TilesAtZoom / (2 * Math.PI));
		result = (2 * Math.atan(Math.exp(result)) - Math.PI / 2) * 180
				/ Math.PI;
		double Zu = result / (180 / Math.PI);
		double yy = (tileNumberY - TilesAtZoom / 2);

		double Zum1 = Zu;
		Zu = Math.asin(1 - ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct * Math.sin(Zum1), FExct))
				/ (Math.exp((2 * yy) / -(TilesAtZoom / (2 * Math.PI))) * Math.pow(1 + FExct * Math.sin(Zum1), FExct)));
		while (Math.abs(Zum1 - Zu) >= MerkElipsK) {
			Zum1 = Zu;
			Zu = Math.asin(1 - ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct * Math.sin(Zum1), FExct))
					/ (Math.exp((2 * yy) / -(TilesAtZoom / (2 * Math.PI))) * Math.pow(1 + FExct * Math.sin(Zum1), FExct)));
		}

		return Zu * 180 / Math.PI;
	}


	public static double getTileDistanceWidth(float zoom) {
		LatLon ll = new LatLon(30, MapUtils.getLongitudeFromTile(zoom, 0));
		LatLon ll2 = new LatLon(30, MapUtils.getLongitudeFromTile(zoom, 1));
		return getDistance(ll, ll2);
	}

	public static double getLongitudeFromTile(double zoom, double x) {
		return x / getPowZoom(zoom) * 360.0 - 180.0;
	}

	public static double getPowZoom(double zoom) {
		if (zoom >= 0 && zoom - Math.floor(zoom) < 0.001f) {
			return 1 << ((int) zoom);
		} else {
			return Math.pow(2, zoom);
		}
	}


	public static float calcDiffPixelX(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize) {
		return (rotateCos * dTileX - rotateSin * dTileY) * tileSize;
	}

	public static float calcDiffPixelY(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize) {
		return (rotateSin * dTileX + rotateCos * dTileY) * tileSize;
	}

	public static double getLatitudeFromTile(float zoom, double y) {
		int sign = y < 0 ? -1 : 1;
		return Math.atan(sign * Math.sinh(Math.PI * (1 - 2 * y / getPowZoom(zoom)))) * 180d / Math.PI;
	}


	public static int getPixelShiftX(float zoom, double long1, double long2, double tileSize) {
		return (int) ((getTileNumberX(zoom, long1) - getTileNumberX(zoom, long2)) * tileSize);
	}


	public static int getPixelShiftY(float zoom, double lat1, double lat2, double tileSize) {
		return (int) ((getTileNumberY(zoom, lat1) - getTileNumberY(zoom, lat2)) * tileSize);
	}


	public static void sortListOfMapObject(List<? extends MapObject> list, final double lat, final double lon) {
		Collections.sort(list, new Comparator<MapObject>() {
			@Override
			public int compare(MapObject o1, MapObject o2) {
				return Double.compare(MapUtils.getDistance(o1.getLocation(), lat, lon), MapUtils.getDistance(o2.getLocation(),
						lat, lon));
			}
		});
	}

	public static String buildGeoUrl(double latitude, double longitude, int zoom) {
		return "geo:" + ((float) latitude) + "," + ((float) longitude) + "?z=" + zoom;
	}

	// Examples
//	System.out.println(buildShortOsmUrl(51.51829d, 0.07347d, 16)); // https://osm.org/go/0EEQsyfu
//	System.out.println(buildShortOsmUrl(52.30103d, 4.862927d, 18)); // https://osm.org/go/0E4_JiVhs
//	System.out.println(buildShortOsmUrl(40.59d, -115.213d, 9)); // https://osm.org/go/TelHTB--
	public static String buildShortOsmUrl(double latitude, double longitude, int zoom) {
		return BASE_SHORT_OSM_URL + createShortLinkString(latitude, longitude, zoom) + "?m";
	}

	public static String createShortLinkString(double latitude, double longitude, int zoom) {
		long lat = (long) (((latitude + 90d)/180d)*(1L << 32));
		long lon = (long) (((longitude + 180d)/360d)*(1L << 32));
		long code = interleaveBits(lon, lat);
		String str = "";
		// add eight to the zoom level, which approximates an accuracy of one pixel in a tile.
		for (int i = 0; i < Math.ceil((zoom + 8) / 3d); i++) {
			str += intToBase64[(int) ((code >> (58 - 6 * i)) & 0x3f)];
		}
		// append characters onto the end of the string to represent
		// partial zoom levels (characters themselves have a granularity of 3 zoom levels).
		for (int j = 0; j < (zoom + 8) % 3; j++) {
			str += '-';
		}
		return str;
	}

	public static GeoParsedPoint decodeShortLinkString(String s) {
		// convert old shortlink format to current one
		s = s.replaceAll("@", "~");
		int i = 0;
		long x = 0;
		long y = 0;
		int z = -8;

		for (i = 0; i < s.length(); i++) {
			int digit = -1;
			char c = s.charAt(i);
			for (int j = 0; j < intToBase64.length; j++)
				if (c == intToBase64[j]) {
					digit = j;
					break;
				}
			if (digit < 0)
				break;
			if (digit < 0)
				break;
			// distribute 6 bits into x and y
			x <<= 3;
			y <<= 3;
			for (int j = 2; j >= 0; j--) {
				x |= ((digit & (1 << (j + j + 1))) == 0 ? 0 : (1 << j));
				y |= ((digit & (1 << (j + j))) == 0 ? 0 : (1 << j));
			}
			z += 3;
		}
		double lon = x * Math.pow(2, 2 - 3 * i) * 90. - 180;
		double lat = y * Math.pow(2, 2 - 3 * i) * 45. - 90;
		// adjust z
		if (i < s.length() && s.charAt(i) == '-') {
			z -= 2;
			if (i + 1 < s.length() && s.charAt(i + 1) == '-')
				z++;
		}
		return new GeoParsedPoint(lat, lon, z);
	}

	/**
	 * interleaves the bits of two 32-bit numbers. the result is known as a Morton code.
	 */
	public static long interleaveBits(long x, long y) {
		long c = 0;
		for (byte b = 31; b >= 0; b--) {
			c = (c << 1) | ((x >> b) & 1);
			c = (c << 1) | ((y >> b) & 1);
		}
		return c;
	}

	/**
	 * Calculate rotation diff D, that R (rotate) + D = T (targetRotate)
	 * D is between -180, 180
	 *
	 * @param rotate
	 * @param targetRotate
	 * @return
	 */
	public static float unifyRotationDiff(float rotate, float targetRotate) {
		float d = targetRotate - rotate;
		while (d >= 180) {
			d -= 360;
		}
		while (d < -180) {
			d += 360;
		}
		return d;
	}

	/**
	 * Calculate rotation diff D, that R (rotate) + D = T (targetRotate)
	 * D is between -180, 180
	 *
	 * @param rotate
	 * @return
	 */
	public static float unifyRotationTo360(float rotate) {
		while (rotate < -180) {
			rotate += 360;
		}
		while (rotate > +180) {
			rotate -= 360;
		}
		return rotate;
	}

	/**
	 * @param diff align difference between 2 angles ]-PI, PI]
	 * @return
	 */
	public static double alignAngleDifference(double diff) {
		while (diff > Math.PI) {
			diff -= 2 * Math.PI;
		}
		while (diff <= -Math.PI) {
			diff += 2 * Math.PI;
		}
		return diff;

	}

	/**
	 * diff align difference between 2 angles [-180, 180]
	 *
	 * @return
	 */
	public static double degreesDiff(double a1, double a2) {
		double diff = a1 - a2;
		while (diff > 180) {
			diff -= 360;
		}
		while (diff <= -180) {
			diff += 360;
		}
		return diff;
	}


	private static double[] coefficientsY = new double[1024];
	private static boolean initializeYArray = false;

	public static double convert31YToMeters(int y1, int y2, int x) {
		int power = 10;
		int pw = 1 << power;
		if (!initializeYArray) {
			coefficientsY[0] = 0;
			for (int i = 0; i < pw - 1; i++) {
				coefficientsY[i + 1] = coefficientsY[i]
						+ measuredDist31(0, i << (31 - power), 0, ((i + 1) << (31 - power)));
			}
			initializeYArray = true;
		}
		int div = 1 << (31 - power);
		int div1 = y1 / div;
		int mod1 = y1 % div;
		int div2 = y2 / div;
		int mod2 = y2 % div;
		double h1 ;
		if(div1 + 1 >= coefficientsY.length) {
			h1 = coefficientsY[div1] + mod1 / (double) div * (coefficientsY[div1] - coefficientsY[div1 - 1]);
		} else {
			h1 = coefficientsY[div1] + mod1 / (double) div * (coefficientsY[div1 + 1] - coefficientsY[div1]);
		}
		double h2 ;
		if(div2 + 1 >= coefficientsY.length) {
			h2 = coefficientsY[div2] + mod2 / (double) div * (coefficientsY[div2] - coefficientsY[div2 - 1]);
		} else {
			h2 = coefficientsY[div2] + mod2 / (double) div * (coefficientsY[div2 + 1] - coefficientsY[div2]);
		}
		double res = h1 - h2;
		return res;
	}

	private static double[] coefficientsX = new double[1024];
	public static double convert31XToMeters(int x1, int x2, int y) {
		int ind = y >> (31 - 10);
		if(coefficientsX[ind] == 0) {
			double md = MapUtils.measuredDist31(x1, y, x2, y);
			if(md < 10) {
				return md;
			}
			coefficientsX[ind] = md / Math.abs(x1 - x2);
		}
		// translate into meters 
		return (x1 - x2) * coefficientsX[ind];
	}


	public static QuadPoint getProjectionPoint31(int px, int py, int st31x, int st31y, int end31x, int end31y) {
		double projection = calculateProjection31TileMetric(st31x, st31y, end31x,
				end31y, px, py);
		double mDist = measuredDist31(end31x, end31y, st31x,
				st31y);
		int pry = end31y;
		int prx = end31x;
		if (projection < 0) {
			prx = st31x;
			pry = st31y;
		} else if (projection >= mDist * mDist) {
			prx = end31x;
			pry = end31y;
		} else {
			prx = (int) (st31x + (end31x - st31x)
					* (projection / (mDist * mDist)));
			pry = (int) (st31y + (end31y - st31y)
					* (projection / (mDist * mDist)));
		}
		return new QuadPoint(prx, pry);
	}


	public static double squareRootDist31(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = MapUtils.convert31YToMeters(y1, y2, x1);
		double dx = MapUtils.convert31XToMeters(x1, x2, y1);
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static double measuredDist31(int x1, int y1, int x2, int y2) {
		return getDistance(MapUtils.get31LatitudeY(y1), MapUtils.get31LongitudeX(x1), MapUtils.get31LatitudeY(y2), MapUtils.get31LongitudeX(x2));
	}

	public static double squareDist31TileMetric(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = convert31YToMeters(y1, y2, x1);
		double dx = convert31XToMeters(x1, x2, y1);
		return dx * dx + dy * dy;
	}

	public static double calculateProjection31TileMetric(int xA, int yA, int xB, int yB, int xC, int yC) {
		// Scalar multiplication between (AB, AC)
		double multiple = MapUtils.convert31XToMeters(xB, xA, yA) * MapUtils.convert31XToMeters(xC, xA, yA) +
				MapUtils.convert31YToMeters(yB, yA, xA) * MapUtils.convert31YToMeters(yC, yA, xA);
		return multiple;
	}

	public static boolean rightSide(double lat, double lon,
			double aLat, double aLon,
			double bLat, double bLon) {
		double ax = aLon - lon;
		double ay = aLat - lat;
		double bx = bLon - lon;
		double by = bLat - lat;
		double sa = ax * by - bx * ay;
		return sa < 0;
	}

	public static long deinterleaveY(long coord) {
		long x = 0;
		for (byte b = 31; b >= 0; b--) {
			x = (x << 1) | (1 & coord >> (b * 2));
		}
		return x;
	}
	
	public static long deinterleaveX(long coord) {
		long x = 0;
		for (byte b = 31; b >= 0; b--) {
			x = (x << 1) | (1 & coord >> (b * 2 + 1));
		}
		return x;
	}

	public static QuadRect calculateLatLonBbox(double latitude, double longitude, int radiusMeters) {
		int zoom = 16;
		float coeff = (float) (radiusMeters / MapUtils.getTileDistanceWidth(zoom));
		double tx = MapUtils.getTileNumberX(zoom, longitude);
		double ty = MapUtils.getTileNumberY(zoom, latitude);
		double topLeftX = Math.max(0, tx - coeff);
		double topLeftY = Math.max(0, ty - coeff);
		int max = (1 << zoom)  - 1;
		double bottomRightX = Math.min(max, tx + coeff);
		double bottomRightY = Math.min(max, ty + coeff);
		double pw = MapUtils.getPowZoom(31 - zoom);
		QuadRect rect = new QuadRect(topLeftX * pw, topLeftY * pw, bottomRightX * pw, bottomRightY * pw);
		rect.left = MapUtils.get31LongitudeX((int) rect.left);
		rect.top = MapUtils.get31LatitudeY((int) rect.top);
		rect.right = MapUtils.get31LongitudeX((int) rect.right);
		rect.bottom = MapUtils.get31LatitudeY((int) rect.bottom);
		return rect;
	}

	public static float getInterpolatedY(float x1, float y1, float x2, float y2, float x) {

		float a = y1 - y2;
		float b = x2 - x1;

		float d = -a * b;
		if (d != 0) {
			float c1 = y2 * x1 - x2 * y1;
			float c2 = x * (y2 - y1);

			return (a * (c1 - c2)) / d;
		} else {
			return y1;
		}
	}

	public static void insetLatLonRect(QuadRect r, double latitude, double longitude) {
		if (r.left == 0 && r.right == 0) {
			r.left = longitude;
			r.right = longitude;
			r.top = latitude;
			r.bottom = latitude;
		} else {
			r.left = Math.min(r.left, longitude);
			r.right = Math.max(r.right, longitude);
			r.top = Math.max(r.top, latitude);
			r.bottom = Math.min(r.bottom, latitude);
		}
	}
}


