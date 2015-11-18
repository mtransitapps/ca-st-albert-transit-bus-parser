package org.mtransit.parser.ca_st_albert_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

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

	private static final long RID_A = 1000l;
	private static final long RID_B = 2000l;
	private static final long RID_F = 6000l;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteId())) {
			return Long.parseLong(gRoute.getRouteId());
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteId());
		matcher.find();
		long id = Long.parseLong(matcher.group());
		if (gRoute.getRouteId().startsWith(A)) {
			return RID_A + id;
		} else if (gRoute.getRouteId().startsWith(B)) {
			return RID_B + id;
		} else if (gRoute.getRouteId().startsWith(F)) {
			return RID_F + id;
		}
		System.out.printf("\nUnexpected route ID %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String RSN_FMS = "FMS";
	private static final String RSN_BL = "BL";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			if (RID_F1.equals(gRoute.getRouteId())) {
				return RSN_FMS;
			} else if (RID_B1.equals(gRoute.getRouteId())) {
				return RSN_BL;
			}
		}
		return super.getRouteShortName(gRoute);
	}

	private static final String EDMONTON = "Edm";
	private static final String GOV_CTR = "Gov Ctr";
	private static final String ST_ALBERT = "St Albert";
	private static final String U_OF_ALBERTA = "U of Alberta";
	private static final String VILLAGE_TRANSIT_STATION = "Vlg Transit Sta";
	private static final String VILLAGE_TRANSIT_STATION_SHORT = "V.T.S.";
	private static final String WEST_EDMONTON_MALL = "West " + EDMONTON + " Mall";
	private static final String EDM_CITY_HALL = EDMONTON + " City Hall";
	private static final String HERITAGE_LKS = "Heritage Lks";
	private static final String GRANDIN = "Grandin";
	private static final String NORTH_RIDGE = "North Rdg";
	private static final String CAMPBELL = "Campbell";
	private static final String WOODLANDS = "Woodlands";
	private static final String COSTCO = "Costco";
	private static final String ENJOY_CENTER = "Enjoy Ctr";
	private static final String PINEVIEW = "Pineview";

	// @formatter:off
	private static final String RLN_A1 = "Mission - St Anne - Grandin - Heritage - " + VILLAGE_TRANSIT_STATION_SHORT;
	private static final String RLN_A2 = HERITAGE_LKS + " - " + VILLAGE_TRANSIT_STATION;
	private static final String RLN_A3 = GRANDIN + " - " + VILLAGE_TRANSIT_STATION;
	private static final String RLN_A4 = "Mission - Lacombe Pk Ests - N Rdg - Deer Rdg - Lacombe E";
	private static final String RLN_A5 = "Lacombe E - Deer Rdg - N Rdg - Lacombe Pk Ests - Mission";
	private static final String RLN_A6 = "Lacombe E - Deer Rdg - N Rdg";
	private static final String RLN_A7 = "Hosp - Erin Rdg - Oakmont";
	private static final String RLN_A8 = "Oakmont - Erin Rdg - Hosp";
	private static final String RLN_A9 = "Braeside - Woodlands - Kingswood - Forest Lawn - Sturgeon";
	private static final String RLN_A10 = "Braeside - Forest Lawn - " + PINEVIEW + " - Sturgeon";
	private static final String RLN_A11 = "Akinsdale - " + CAMPBELL + " - " + PINEVIEW + " - Woodlands - Perron St";
	private static final String RLN_A12 = "Akinsdale - " + PINEVIEW + " - " + CAMPBELL;
	private static final String RLN_A13 = "Akinsdale - " + PINEVIEW + " - " + WOODLANDS;
	private static final String RLN_A21 = ENJOY_CENTER + " - Riel -  St Anne - " + VILLAGE_TRANSIT_STATION_SHORT;
	private static final String RLN_B1 = "Botanical Loop";
	private static final String RLN_F1 = "Farmers Mkt Shuttle";
	private static final String RLN_A14 = VILLAGE_TRANSIT_STATION_SHORT + " - St Albert Ctr - Summit Ctr - Sturgeon Hosp - " + COSTCO;
	private static final String RLN_201 = /* ST_ALBERT + " - " + */EDMONTON + " via Kingsway"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_202 = /* ST_ALBERT + " - " + */EDMONTON + " via NAIT & MacEwan"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_203 = /* ST_ALBERT + " - " + */U_OF_ALBERTA + " via Westmount"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_204 = /* ST_ALBERT + " - " + */U_OF_ALBERTA + " Express"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_205 = /* ST_ALBERT + " - " + */WEST_EDMONTON_MALL + ""/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_208 = /* ST_ALBERT + " - " + */GOV_CTR + " via MacEwan Express"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_209 = /* ST_ALBERT + " - " +*/ EDMONTON + " via NAIT & MacEwan Express - " + GOV_CTR + ""/* + " - " + COMMUTER_SERVICE*/;
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
		if (!Utils.isDigitsOnly(gRoute.getRouteId())) {
			// @formatter:off
			if (RID_A1.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A1;
			} else if (RID_A2.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A2;
			} else if (RID_A3.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A3;
			} else if (RID_A4.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A4;
			} else if (RID_A5.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A5;
			} else if (RID_A6.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A6;
			} else if (RID_A7.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A7;
			} else if (RID_A8.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A8;
			} else if (RID_A9.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A9;
			} else if (RID_A10.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A10;
			} else if (RID_A11.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A11;
			} else if (RID_A12.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A12;
			} else if (RID_A13.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A13;
			} else if (RID_A14.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A14;
			} else if (RID_A21.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_A21;
			} else if (RID_B1.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_B1;
			} else if (RID_F1.equalsIgnoreCase(gRoute.getRouteId())) { return RLN_F1;
			}
			// @formatter:on
		} else {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
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
		System.out.printf("\nUnexpected route long name %s!\n", gRoute);
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
		if (!Utils.isDigitsOnly(gRoute.getRouteId())) {
			if (RID_B1.equalsIgnoreCase(gRoute.getRouteId())) {
				if (gRoute.getRouteColor().equalsIgnoreCase(COLOR_ECFE25)) {
					return COLOR_702929;
				}
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static final String EXCHANGE = "Exch";
	private static final String ST_ALBERT_EXCHANGE_CENTER = ST_ALBERT + " " + EXCHANGE + " Ctr";

	private static final String BOTANICAL_PARK = "Botanical Pk";
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
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(201l, new RouteTripSpec(201l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ALBERT_EXCHANGE_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_CITY_HALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1123", "1989", "0959", "0962", "0971" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "0971", "0959", "6272", "1123" })) //
				.compileBothTripSort());
		map2.put(202l, new RouteTripSpec(202l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ALBERT_EXCHANGE_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_CITY_HALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1364", "1227", "6152", "0960", "0952", "0971" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "0971", "0960", "1824", "1364" })) //
				.compileBothTripSort());
		map2.put(204l, new RouteTripSpec(204l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ALBERT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, U_OF_ALBERTA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2636", "2749", "0962", "0971" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "0971", "0175", "2636" })) //
				.compileBothTripSort());
		map2.put(208l, new RouteTripSpec(208l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ALBERT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1302", "1898", "0972" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "0972", "1643", "1304" })) //
				.compileBothTripSort());
		map2.put(RID_A + 2l, new RouteTripSpec(RID_A + 2l, // A2
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HERITAGE_LKS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "0956", "0645", "0515" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "0515", "0743", "0956" })) //
				.compileBothTripSort());
		map2.put(RID_A + 3l, new RouteTripSpec(RID_A + 3l, // A3
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GRANDIN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "0956", "0041", "0053" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "0053", "0061", "0956" })) //
				.compileBothTripSort());
		map2.put(RID_A + 6l, new RouteTripSpec(RID_A + 6l, // A6
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTH_RIDGE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "0220", "0427", "0973", "0952" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "0952", "0973", "0497", "0220" })) //
				.compileBothTripSort());
		map2.put(RID_A + 10l, new RouteTripSpec(RID_A + 10l, // A10
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PINEVIEW) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "0221", "0267", "0955" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "0955", "0205", "0221" })) //
				.compileBothTripSort());
		map2.put(RID_A + 12l, new RouteTripSpec(RID_A + 12l, // A12
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAMPBELL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "0954", "0337", "0603" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "0603", "0315", "0954" })) //
				.compileBothTripSort());
		map2.put(RID_A + 13l, new RouteTripSpec(RID_A + 13l, // A13
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WOODLANDS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "0954", "0331", "0575" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "0575", "0307", "0954" })) //
				.compileBothTripSort());
		map2.put(RID_A + 14l, new RouteTripSpec(RID_A + 14l, // A14
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COSTCO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "0951", "0065", "0974", "0909", "0925", "0250" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "0250", "0925", "0971", "0113", "0951" })) //
				.compileBothTripSort());
		map2.put(RID_A + 21l, new RouteTripSpec(RID_A + 21l, // A21
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VILLAGE_TRANSIT_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ENJOY_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "0252", "0094", "0953" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "0953", "0113", "0252" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		String gTripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
		if (mRoute.getId() == 203l) {
			if (gTripHeadsignLC.equals(TO_ST_ALBERT)) {
				mTrip.setHeadsignString(ST_ALBERT, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_UNIVERSITY_OF_ALBERTA)) {
				mTrip.setHeadsignString(U_OF_ALBERTA, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == 205l) {
			if (gTripHeadsignLC.equals(TO_ST_ALBERT)) {
				mTrip.setHeadsignString(ST_ALBERT, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_WEST_EDMONTON_MALL)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == 209l) {
			if (gTripHeadsignLC.equals(TO_ST_ALBERT)) {
				mTrip.setHeadsignString(ST_ALBERT, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_GOVERNMENT_CENTRE)) {
				mTrip.setHeadsignString(GOV_CTR, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 1l) { // A1
			if (gTripHeadsignLC.equals(TO_ST_ALBERT_EXCHANGE_CENTRE)) {
				mTrip.setHeadsignString(ST_ALBERT_EXCHANGE_CENTER, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_VILLAGE_TRANSIT_STATION)) {
				mTrip.setHeadsignString(VILLAGE_TRANSIT_STATION, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 4l) { // A4
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(CLOCKWISE, gTrip.getDirectionId());
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 5l) { // A5
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(COUNTER_CLOCKWISE, gTrip.getDirectionId());
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 7l) { // A7
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(CLOCKWISE, gTrip.getDirectionId());
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 8l) { // A8
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(COUNTER_CLOCKWISE, gTrip.getDirectionId());
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 9l) { // A9
			if (gTripHeadsignLC.equals(TO_ST_ALBERT_EXCHANGE)) {
				mTrip.setHeadsignString(ST_ALBERT_EXCHANGE_CENTER, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_VILLAGE_TRANSIT_STATION)) {
				mTrip.setHeadsignString(VILLAGE_TRANSIT_STATION, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_A + 11l) { // A11
			if (gTripHeadsignLC.equals(TO_ST_ALBERT_EXCHANGE)) {
				mTrip.setHeadsignString(ST_ALBERT_EXCHANGE_CENTER, 0);
				return;
			} else if (gTripHeadsignLC.equals(TO_VILLAGE_TRANSIT_STATION)) {
				mTrip.setHeadsignString(VILLAGE_TRANSIT_STATION, 1);
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		} else if (mRoute.getId() == RID_B + 1l) { // B1
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(BOTANICAL_PARK, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(ENJOY_CENTER, gTrip.getDirectionId());
				return;
			}
			System.out.printf(UNEXPECTED_TRIP_ROUTE_ID_S_S, mRoute.getId(), gTrip);
			System.exit(-1);
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String TO_START_WITH = "to ";

	private static final Pattern EXCHANGE_ = Pattern.compile("(exchange)", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = EXCHANGE;

	private static final Pattern GOVERNMENT_CENTRE = Pattern.compile("(government (centre|center))", Pattern.CASE_INSENSITIVE);
	private static final String GOVERNMENT_CENTRE_REPLACEMENT = GOV_CTR;

	private static final Pattern UNIVERSITY_OF_ALBERTA = Pattern.compile("(university Of alberta)", Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_ALBERTA_REPLACEMENT = U_OF_ALBERTA;

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (tripHeadsign.toLowerCase(Locale.ENGLISH).startsWith(TO_START_WITH)) {
			tripHeadsign = tripHeadsign.substring(TO_START_WITH.length());
		}
		tripHeadsign = EXCHANGE_.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = GOVERNMENT_CENTRE.matcher(tripHeadsign).replaceAll(GOVERNMENT_CENTRE_REPLACEMENT);
		tripHeadsign = UNIVERSITY_OF_ALBERTA.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_ALBERTA_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern SIR_WINSTON_CHURCHILL = Pattern.compile("S.W.C.", Pattern.CASE_INSENSITIVE);
	private static final String SIR_WINSTON_CHURCHILL_REPLACEMENT = "Sir Winston Churchill";

	private static final Pattern AGLC = Pattern.compile("A.G.L.C.", Pattern.CASE_INSENSITIVE);
	private static final String AGLC_REPLACEMENT = "AGLC";

	private static final Pattern NAIT = Pattern.compile("N.A.I.T.", Pattern.CASE_INSENSITIVE);
	private static final String NAIT_REPLACEMENT = "NAIT";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = EXCHANGE_.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = GOVERNMENT_CENTRE.matcher(gStopName).replaceAll(GOVERNMENT_CENTRE_REPLACEMENT);
		gStopName = UNIVERSITY_OF_ALBERTA.matcher(gStopName).replaceAll(UNIVERSITY_OF_ALBERTA_REPLACEMENT);
		gStopName = SIR_WINSTON_CHURCHILL.matcher(gStopName).replaceAll(SIR_WINSTON_CHURCHILL_REPLACEMENT);
		gStopName = AGLC.matcher(gStopName).replaceAll(AGLC_REPLACEMENT);
		gStopName = NAIT.matcher(gStopName).replaceAll(NAIT_REPLACEMENT);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
