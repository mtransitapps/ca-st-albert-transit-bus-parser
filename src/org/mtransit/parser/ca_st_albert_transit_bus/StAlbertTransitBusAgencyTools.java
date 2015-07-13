package org.mtransit.parser.ca_st_albert_transit_bus;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://stalbert.ca/getting-around/stat-transit/rider-tools/open-data-gtfs/
// http://stalbert.ca/uploads/files-zip/google_transit.zip
public class StAlbertTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-st-albert-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new StAlbertTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating StAT bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating StAT bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String F = "F";

	@Override
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.route_id)) {
			return Long.parseLong(gRoute.route_id);
		}
		Matcher matcher = DIGITS.matcher(gRoute.route_id);
		matcher.find();
		int id = Integer.parseInt(matcher.group());
		if (gRoute.route_id.startsWith(A)) {
			return 1000 + id;
		} else if (gRoute.route_id.startsWith(B)) {
			return 2000 + id;
		} else if (gRoute.route_id.startsWith(F)) {
			return 6000 + id;
		}
		System.out.println("Unexpected route ID " + gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String RSN_FMS = "FMS";
	private static final String RSN_BL = "BL";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.route_short_name)) {
			if (RID_F1.equals(gRoute.route_id)) {
				return RSN_FMS;
			} else if (RID_B1.equals(gRoute.route_id)) {
				return RSN_BL;
			}
		}
		return super.getRouteShortName(gRoute);
	}

	private static final String EDMONTON = "Edm.";
	private static final String GOVERNMENT_CENTER = "Gov Ctr";
	private static final String ST_ALBERT = "St Albert";
	private static final String UNIVERSITY_OF_ALBERTA = "U of Alberta";
	private static final String VILLAGE_TRANSIT_STATION = "Vlg Transit Sta";
	private static final String VILLAGE_TRANSIT_STATION_SHORT = "V.T.S.";
	private static final String WEST_EDMONTON_MALL = "West " + EDMONTON + " Mall";

	// @formatter:off
	private static final String RLN_A1 = "Mission - St Anne - Grandin - Heritage - " + VILLAGE_TRANSIT_STATION_SHORT;
	private static final String RLN_A2 = "Heritage Lks - " + VILLAGE_TRANSIT_STATION;
	private static final String RLN_A3 = "Grandin - " + VILLAGE_TRANSIT_STATION;
	private static final String RLN_A4 = "Mission - Lacombe Pk Ests - N Rdg - Deer Rdg - Lacombe E";
	private static final String RLN_A5 = "Lacombe E - Deer Rdg - N Rdg - Lacombe Pk Ests - Mission";
	private static final String RLN_A6 = "Lacombe E - Deer Rdg - N Rdg";
	private static final String RLN_A7 = "Hosp - Erin Rdg - Oakmont";
	private static final String RLN_A8 = "Oakmont - Erin Rdg - Hosp";
	private static final String RLN_A9 = "Braeside - Woodlands - Kingswood - Forest Lawn - Sturgeon";
	private static final String RLN_A10 = "Braeside - Forest Lawn - Sturgeon";
	private static final String RLN_A11 = "Akinsdale - Pineview - Woodlands - Perron St";
	private static final String RLN_A12 = "Akinsdale - Pineview";
	private static final String RLN_A13 = "Akinsdale - Pineview - Woodlands";
	private static final String RLN_A21 = "Enjoy Ctr - Riel -  St Anne - " + VILLAGE_TRANSIT_STATION;
	private static final String RLN_B1 = "Botanical Loop";
	private static final String RLN_F1 = "Farmers Mkt Shuttle";
	private static final String RLN_A14 = VILLAGE_TRANSIT_STATION_SHORT + " - St Albert Ctr - Summit Ctr - Sturgeon Hosp - Costco";
	private static final String RLN_201 = /* ST_ALBERT + " - " + */EDMONTON + " via Kingsway"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_202 = /* ST_ALBERT + " - " + */EDMONTON + " via NAIT & MacEwan"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_203 = /* ST_ALBERT + " - " + */UNIVERSITY_OF_ALBERTA + " via Westmount"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_204 = /* ST_ALBERT + " - " + */UNIVERSITY_OF_ALBERTA + " Express"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_205 = /* ST_ALBERT + " - " + */WEST_EDMONTON_MALL + ""/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_208 = /* ST_ALBERT + " - " + */GOVERNMENT_CENTER + " via MacEwan Express"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_209 = /* ST_ALBERT + " - " +*/ EDMONTON + " via NAIT & MacEwan Express - " + GOVERNMENT_CENTER + ""/* + " - " + COMMUTER_SERVICE*/;
	private static final String RLN_211 = /* ST_ALBERT + " - " +*/ EDMONTON + " via MacEwan Express"/* + " - " + COMMUTER_SERVICE*/;
	// @formatter:on

	private static final String RID_A1 = "A1";
	private static final String RID_A2 = "A2";
	private static final String RID_A3 = "A3";
	private static final String RID_A4 = "A4";
	private static final String RID_A5 = "A5";
	private static final String RID_A6 = "A6";
	private static final String RID_A7 = "A7";
	private static final String RID_A8 = "A8";
	private static final String RID_A9 = "A9";
	private static final String RID_A10 = "A10";
	private static final String RID_A11 = "A11";
	private static final String RID_A12 = "A12";
	private static final String RID_A13 = "A13";
	private static final String RID_A14 = "A14";
	private static final String RID_A21 = "A21";
	private static final String RID_B1 = "B1";
	private static final String RID_F1 = "F1";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.route_id)) {
			// @formatter:off
			if (RID_A1.equalsIgnoreCase(gRoute.route_id)) { return RLN_A1;
			} else if (RID_A2.equalsIgnoreCase(gRoute.route_id)) { return RLN_A2;
			} else if (RID_A3.equalsIgnoreCase(gRoute.route_id)) { return RLN_A3;
			} else if (RID_A4.equalsIgnoreCase(gRoute.route_id)) { return RLN_A4;
			} else if (RID_A5.equalsIgnoreCase(gRoute.route_id)) { return RLN_A5;
			} else if (RID_A6.equalsIgnoreCase(gRoute.route_id)) { return RLN_A6;
			} else if (RID_A7.equalsIgnoreCase(gRoute.route_id)) { return RLN_A7;
			} else if (RID_A8.equalsIgnoreCase(gRoute.route_id)) { return RLN_A8;
			} else if (RID_A9.equalsIgnoreCase(gRoute.route_id)) { return RLN_A9;
			} else if (RID_A10.equalsIgnoreCase(gRoute.route_id)) { return RLN_A10;
			} else if (RID_A11.equalsIgnoreCase(gRoute.route_id)) { return RLN_A11;
			} else if (RID_A12.equalsIgnoreCase(gRoute.route_id)) { return RLN_A12;
			} else if (RID_A13.equalsIgnoreCase(gRoute.route_id)) { return RLN_A13;
			} else if (RID_A14.equalsIgnoreCase(gRoute.route_id)) { return RLN_A14;
			} else if (RID_A21.equalsIgnoreCase(gRoute.route_id)) { return RLN_A21;
			} else if (RID_B1.equalsIgnoreCase(gRoute.route_id)) { return RLN_B1;
			} else if (RID_F1.equalsIgnoreCase(gRoute.route_id)) { return RLN_F1;
			}
			// @formatter:on
		} else {
			int rsn = Integer.parseInt(gRoute.route_short_name);
			switch (rsn) {
			// @formatter:off
			case 201: return RLN_201;
			case 202: return RLN_202;
			case 203: return RLN_203;
			case 204: return RLN_204;
			case 205: return RLN_205;
			case 208: return RLN_208;
			case 209: return RLN_209;
			case 211: return RLN_211;
			// @formatter:on
			}
		}
		System.out.println("Unexpected route long name " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final String AGENCY_COLOR_GREEN = "4AA942"; // GREEN (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_ECFE25 = "ecfe25";
	private static final String COLOR_702929 = "702929";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.route_id)) {
			if (RID_B1.equalsIgnoreCase(gRoute.route_id)) {
				if (gRoute.route_color.equalsIgnoreCase(COLOR_ECFE25)) {
					return COLOR_702929;
				}
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static final String EXCHANGE = "Ex";
	private static final String ST_ALBERT_EXCHANGE_CENTER = ST_ALBERT + " " + EXCHANGE + " Ctr";

	private static final String LOOP = "Loop";
	private static final String BOTANICAL_PARK = "Botanical Pk";
	private static final String ENJOY_CENTER = "Enjoy Ctr";
	private static final String COUNTER_CLOCKWISE = "Counter-Clockwise";
	private static final String CLOCKWISE = "Clockwise";

	private static final String UNEXPECTED_TRIP_ROUTE_ID_S_S = "Unexpected trip (route ID: %s): %s\n";

	private static final String TO_ST_ALBERT_EXCHANGE = "to st. albert exchange";
	private static final String TO_VILLAGE_TRANSIT_STATION = "to village transit station";
	private static final String TO_ST_ALBERT_EXCHANGE_CENTRE = "to st. albert exchange centre";
	private static final String TO_GOVERNMENT_CENTRE = "to government centre";
	private static final String TO_WEST_EDMONTON_MALL = "to west edmonton mall";
	private static final String TO_UNIVERSITY_OF_ALBERTA = "to university of alberta";
	private static final String TO_ST_ALBERT = "to st. albert";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String gTripHeadsignLC = gTrip.trip_headsign.toLowerCase(Locale.ENGLISH);
		if (mRoute.id == 203l) {
			if (gTripHeadsignLC.equals(TO_ST_ALBERT)) {
				mTrip.setHeadsignString(ST_ALBERT, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_UNIVERSITY_OF_ALBERTA)) {
				mTrip.setHeadsignString(UNIVERSITY_OF_ALBERTA, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 205l) {
			if (gTripHeadsignLC.equals(TO_ST_ALBERT)) {
				mTrip.setHeadsignString(ST_ALBERT, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_WEST_EDMONTON_MALL)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 209l) {
			if (gTripHeadsignLC.equals(TO_ST_ALBERT)) {
				mTrip.setHeadsignString(ST_ALBERT, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_GOVERNMENT_CENTRE)) {
				mTrip.setHeadsignString(GOVERNMENT_CENTER, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1001l) { // A1
			if (gTripHeadsignLC.equals(TO_ST_ALBERT_EXCHANGE_CENTRE)) {
				mTrip.setHeadsignString(ST_ALBERT_EXCHANGE_CENTER, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_VILLAGE_TRANSIT_STATION)) {
				mTrip.setHeadsignString(VILLAGE_TRANSIT_STATION, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1004l) { // A4
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(CLOCKWISE, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1005l) { // A5
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(COUNTER_CLOCKWISE, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1006l) { // A6
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(LOOP, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1007l) { // A7
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(CLOCKWISE, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1008l) { // A8
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(COUNTER_CLOCKWISE, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1009l) { // A9
			if (gTripHeadsignLC.equals(TO_ST_ALBERT_EXCHANGE)) {
				mTrip.setHeadsignString(ST_ALBERT_EXCHANGE_CENTER, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_VILLAGE_TRANSIT_STATION)) {
				mTrip.setHeadsignString(VILLAGE_TRANSIT_STATION, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1011l) { // A11
			if (gTripHeadsignLC.equals(TO_ST_ALBERT_EXCHANGE)) {
				mTrip.setHeadsignString(ST_ALBERT_EXCHANGE_CENTER, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_VILLAGE_TRANSIT_STATION)) {
				mTrip.setHeadsignString(VILLAGE_TRANSIT_STATION, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 1014l) { // A14
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(LOOP, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		} else if (mRoute.id == 2001l) { // B1
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(BOTANICAL_PARK, gTrip.direction_id);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(ENJOY_CENTER, gTrip.direction_id);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.id, gTrip);
			System.exit(-1);
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.trip_headsign), gTrip.direction_id);
	}

	private static final Pattern EXCHANGE_ = Pattern.compile("(exchange)", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = EXCHANGE;

	private static final Pattern ST_P_ALBERT = Pattern.compile("(st. albert)", Pattern.CASE_INSENSITIVE);
	private static final String ST_ALBERT_REPLACEMENT = ST_ALBERT;

	private static final String TO_START_WITH = "to ";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = EXCHANGE_.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = ST_P_ALBERT.matcher(tripHeadsign).replaceAll(ST_ALBERT_REPLACEMENT);
		if (tripHeadsign.toLowerCase(Locale.ENGLISH).startsWith(TO_START_WITH)) {
			tripHeadsign = tripHeadsign.substring(TO_START_WITH.length());
		}
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern AND_POINT = Pattern.compile("((^|\\W){1}(av|ave|bldg|blvd|cl|cr|crt|ct|ctr|dr|hosp|n|pl|rd|s|sch|st|stn|tr)(\\.)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String AND_POINT_REPLACEMENT = "$2$3$5";

	private static final Pattern CLEAN_SLASHES = Pattern.compile("(\\S)[\\s]*[/][\\s]*(\\S)");
	private static final String CLEAN_SLASHES_REPLACEMENT = "$1 / $2";

	private static final Pattern N_P = Pattern.compile(" N\\.", Pattern.CASE_INSENSITIVE);
	private static final String N_P_REPLACEMENT = " N";

	private static final Pattern S_P = Pattern.compile(" S\\.", Pattern.CASE_INSENSITIVE);
	private static final String S_P_REPLACEMENT = " S";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CLEAN_SLASHES.matcher(gStopName).replaceAll(CLEAN_SLASHES_REPLACEMENT);
		gStopName = AND_POINT.matcher(gStopName).replaceAll(AND_POINT_REPLACEMENT);
		gStopName = N_P.matcher(gStopName).replaceAll(N_P_REPLACEMENT);
		gStopName = S_P.matcher(gStopName).replaceAll(S_P_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
