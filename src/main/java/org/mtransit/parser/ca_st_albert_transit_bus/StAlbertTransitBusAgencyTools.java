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
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GIDs;
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

// https://stalbert.ca/city/transit/tools/open-data-gtfs/
// https://stalbert.ca/site/assets/files/3840/google_transit.zip
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
		MTLog.log("Generating StAT bus data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating StAT bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	public void setupNext() {
		// DO NOTHING
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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

	private static final int AGENCY_ID_INT = GIDs.getInt("2"); // St. Albert Transit

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (gRoute.isDifferentAgency(AGENCY_ID_INT)) {
			return true; // exclude
		}
		return super.excludeRoute(gRoute);
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
	private static final String S = "S";

	private static final long RID_A = 1_000L;
	private static final long RID_B = 2_000L;
	private static final long RID_C = 3_000L;
	private static final long RID_C1 = 800_000L + 1L;
	private static final long RID_CH = 800_000L + RID_C;
	private static final long RID_F = 6_000L;
	private static final long RID_I = 9_000L;
	private static final long RID_N = 14_000L;
	private static final long RID_R = 18_000L;
	private static final long RID_S = 19_000L;
	private static final long RID_FA = 600_000L + RID_A;
	private static final long RID_RA = 1_800_000L + RID_A;
	private static final long RID_RR = 1_800_000L + RID_R;
	private static final long RID_SN = 1_900_000L + RID_N;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			if (SNOWFLAKE_FESTIVAL_SHUTTLE.equals(gRoute.getRouteLongName())) {
				return RID_SN;
			}
			if (FIRE_AND_ICE_FESTIVAL.equals(gRoute.getRouteLongName())) {
				return RID_FA;
			}
			throw new MTLog.Fatal("Unexpected route ID %s!", gRoute.toStringPlus());
		}
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName());
		}
		if ("C1".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_C1;
		}
		if ("CH".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_CH;
		}
		if ("FA".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_FA; // Fire and Ice Festival
		}
		if ("RA".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_RA;
		}
		if ("RR".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_RR;
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			long id = Long.parseLong(matcher.group());
			if (gRoute.getRouteShortName().startsWith(A)) {
				return RID_A + id;
			} else if (gRoute.getRouteShortName().startsWith(B)) {
				return RID_B + id;
			} else if (gRoute.getRouteShortName().startsWith(F)) {
				return RID_F + id;
			} else if (gRoute.getRouteShortName().startsWith(S)) {
				return RID_S + id;
			}
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute.toStringPlus());
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			if (RID_F1.equals(gRoute.getRouteShortName())) {
				return "FMS";
			} else if (RID_B1.equals(gRoute.getRouteShortName())) {
				return "BL";
			}
			if (SNOWFLAKE_FESTIVAL_SHUTTLE.equals(gRoute.getRouteLongName())) {
				return "SN";
			}
			if (FIRE_AND_ICE_FESTIVAL.equals(gRoute.getRouteLongName())) {
				return "FA";
			}
			if ("C1".equals(gRoute.getRouteShortName())) {
				return "C1";
			}
			if ("CH".equals(gRoute.getRouteShortName())) {
				return "CH";
			}
			if ("RA".equals(gRoute.getRouteShortName())) {
				return "RA";
			}
			if ("RR".equals(gRoute.getRouteShortName())) {
				return "RR";
			}
			throw new MTLog.Fatal("Unexpected route short name %s!", gRoute);
		}
		return super.getRouteShortName(gRoute);
	}

	private static final String EXCHANGE = "Exch";

	private static final String TRANSIT_CENTRE_SHORT = "TC";

	private static final String EDMONTON = "Edm";
	private static final String GOV_CTR = "Gov Ctr";
	private static final String ST_ALBERT = "St Albert";
	private static final String ST_ALBERT_CENTER = ST_ALBERT + " Ctr";
	private static final String U_OF_ALBERTA = "U of Alberta";
	private static final String VILLAGE_TRANSIT_STATION = "Vlg Transit Sta";
	private static final String VILLAGE_TRANSIT_STATION_SHORT = "V.T.S.";
	private static final String WEST_EDMONTON = "West " + EDMONTON;
	private static final String WEST_EDMONTON_MALL = WEST_EDMONTON + " Mall";
	private static final String HERITAGE_LKS = "Heritage Lks";
	private static final String GRANDIN = "Grandin";
	private static final String CAMPBELL = "Campbell";
	private static final String WOODLANDS = "Woodlands";
	private static final String COSTCO = "Costco";
	private static final String ENJOY_CENTER = "Enjoy Ctr";
	private static final String PINEVIEW = "Pineview";
	private static final String KINGSWOOD = "Kingswood";
	private static final String SNOWFLAKE_FESTIVAL_SHUTTLE = "Snowflake Festival Shuttle";
	private static final String FIRE_AND_ICE_FESTIVAL = "Fire and Ice Festival";

	private static final String ST_ALBERT_EXCHANGE_CENTER = ST_ALBERT + " " + EXCHANGE + " Ctr";

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
	private static final String RLN_A14 = VILLAGE_TRANSIT_STATION_SHORT + " - St Albert Ctr - Summit Ctr - Sturgeon Hosp - " + COSTCO;
	private static final String RLN_A15 = "Dial-a-Bus"; // TODO?
	private static final String RLN_A16 = "Dial-a-Bus"; // TODO?
	private static final String RLN_A21 = ENJOY_CENTER + " - Riel -  St Anne - " + VILLAGE_TRANSIT_STATION_SHORT;
	private static final String RLN_B1 = "Botanical Loop";
	private static final String RLN_C1 = "Canada Day Shuttle";
	private static final String RLN_CH = "Children's Festival Shuttle";
	private static final String RLN_SN = SNOWFLAKE_FESTIVAL_SHUTTLE;
	private static final String RLN_F1 = "Farmers Mkt Shuttle";
	private static final String RLN_RA = "Rock n August";
	private static final String RLN_RR = "Rainmaker Rodeo Shuttle";
	private static final String RLN_201 = /* ST_ALBERT + " - " + */EDMONTON + " via Kingsway"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_202 = /* ST_ALBERT + " - " + */EDMONTON + " via NAIT & MacEwan"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_203 = /* ST_ALBERT + " - " + */U_OF_ALBERTA + " via Westmount"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_204 = /* ST_ALBERT + " - " + */U_OF_ALBERTA + " Express"/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_205 = /* ST_ALBERT + " - " + */WEST_EDMONTON_MALL + ""/* + " - " + COMMUTER_SERVICE */;
	private static final String RLN_207 = /* ST_ALBERT + " - " + */ST_ALBERT + " Express"/* + " - " + COMMUTER_SERVICE */;
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
	private static final String RID_A15 = "A15";
	private static final String RID_A16 = "A16";
	private static final String RID_A21 = "A21";
	private static final String RID_B1 = "B1";
	private static final String RID_F1 = "F1";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				// @formatter:off
				if (RID_A1.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A1;
				} else if (RID_A2.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A2;
				} else if (RID_A3.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A3;
				} else if (RID_A4.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A4;
				} else if (RID_A5.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A5;
				} else if (RID_A6.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A6;
				} else if (RID_A7.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A7;
				} else if (RID_A8.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A8;
				} else if (RID_A9.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A9;
				} else if (RID_A10.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A10;
				} else if (RID_A11.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A11;
				} else if (RID_A12.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A12;
				} else if (RID_A13.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A13;
				} else if (RID_A14.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A14;
				} else if (RID_A15.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A15;
				} else if (RID_A16.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A16;
				} else if (RID_A21.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_A21;
				} else if (RID_B1.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_B1;
				} else if (RID_F1.equalsIgnoreCase(gRoute.getRouteShortName())) { return RLN_F1;
				}
				// @formatter:on
				if ("C1".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return RLN_C1;
				}
				if ("CH".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return RLN_CH;
				}
				if ("RA".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return RLN_RA;
				}
				if ("RR".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return RLN_RR;
				}
			} else {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				// @formatter:off
				case 201: return RLN_201;
				case 202: return RLN_202;
				case 203: return RLN_203;
				case 204: return RLN_204;
				case 205: return RLN_205;
				case 207: return RLN_207;
				case 208: return RLN_208;
				case 209: return RLN_209;
				case 211: return RLN_211;
				// @formatter:on
				}
			}
			throw new MTLog.Fatal("Unexpected route long name %s!", gRoute);
		}
		return super.getRouteLongName(gRoute);
	}

	private static final String AGENCY_COLOR_GREEN = "4AA942"; // GREEN (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
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
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		map2.put(RID_A + 2L, new RouteTripSpec(RID_A + 2L, // A2
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Heritage Lks", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Vlg Sta") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"0956", // Village Transit Station
								"0645", // ++
								"0515" // Harwood Dr & Heritage Blvd
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
						"0515", // Harwood Dr & Heritage Blvd
								"0743", // ++
								"0956" // Village Transit Station
						)) //
				.compileBothTripSort());
		map2.put(RID_A + 3L, new RouteTripSpec(RID_A + 3L, // A3
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Grandin", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Vlg Sta") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"0956", // Village Transit Station
							"0041", //
							"0053" // SWC Ave & Grandin Village I
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
							"0053", // SWC Ave & Grandin Village I
							"0061", //
							"0956" // Village Transit Station
						 )) //
				.compileBothTripSort());
		map2.put(RID_A + 10L, new RouteTripSpec(RID_A + 10L, // A10
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Vlg Sta", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Forest Lawn") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"0221", // Franklin Pl./S.W.C. Av. #ForestLawn
								"0267", //
								"0955" // Village Transit Station
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
						"0955", // Village Transit Station
								"0203", // ==
								"0205", // !=
								"0215", // !=
								"0895", // ==
								"0221" // Franklin Pl./S.W.C. Av. #ForestLawn
						)) //
				.compileBothTripSort());
		map2.put(RID_A + 13L, new RouteTripSpec(RID_A + 13L, // A13
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Woodlands", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Vlg Sta") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
							"0954", // Village Transit Station
							"0331", // ++
							"0575" // Poirier Ave & Kirkwood Dr
						 )) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
							"0575", // Poirier Ave & Kirkwood Dr
							"0307", // ++
							"0954" // Village Transit Station
						 )) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	private static final Pattern STARTS_WITH_A_ = Pattern.compile("(^A)", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_A_REPLACEMENT = StringUtils.EMPTY;

	@Override
	public String cleanStopOriginalId(String gStopId) {
		gStopId = STARTS_WITH_A_.matcher(gStopId).replaceAll(STARTS_WITH_A_REPLACEMENT);
		return gStopId;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		 if (mTrip.getRouteId() == RID_A + 4L) { // A4
			if (gTrip.getTripHeadsign().endsWith(" Lacombe Park Deer Ridge")) {
				mTrip.setHeadsignString(cleanTripHeadsign(
					gTrip.getTripHeadsign().substring(
						0, 
						gTrip.getTripHeadsign().length() - " Lacombe Park Deer Ridge".length()
						)), gTrip.getDirectionId());
				return;
			}
		}
		if (mTrip.getRouteId() == RID_A + 5L) { // A5
			if (gTrip.getTripHeadsign().endsWith(" " + "Lacombe Park")) {
				mTrip.setHeadsignString(cleanTripHeadsign("Lacombe Park"), gTrip.getDirectionId());
				return;
			}
			if (gTrip.getTripHeadsign().endsWith(" " + "Village Station")) {
				mTrip.setHeadsignString(cleanTripHeadsign("Village Station"), gTrip.getDirectionId());
				return;
			}
		}
		if (mTrip.getRouteId() == RID_A + 7L) { // A7
			if (gTrip.getTripHeadsign().endsWith(" " + "Oakmont")) {
				mTrip.setHeadsignString(cleanTripHeadsign("Oakmont"), gTrip.getDirectionId());
				return;
			}
		}
		if (mTrip.getRouteId() == RID_A + 8L) { // A8
			if (gTrip.getTripHeadsign().endsWith(" " + "Erin Ridge")) {
				mTrip.setHeadsignString(cleanTripHeadsign("Erin Ridge"), gTrip.getDirectionId());
				return;
			}
		}
		if (mTrip.getRouteId() == RID_A + 12L) { // A12
			if (gTrip.getTripHeadsign().endsWith(" " + "Campbell")) {
				mTrip.setHeadsignString(cleanTripHeadsign("Campbell"), gTrip.getDirectionId());
				return;
			}
		}
		mTrip.setHeadsignString(
			cleanTripHeadsign(gTrip.getTripHeadsign()),
			gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == RID_A + 4L) { // A4
			if (Arrays.asList( //
					"Vlg Sta", //
					"St Albert Ctr" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Albert Ctr", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == RID_A + 5L) { // A5
			if (Arrays.asList( //
					"Lacombe Pk", //
					"Vlg Sta" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vlg Sta", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_A + 6L) { // A6
			if (Arrays.asList( //
					"Vlg Transit Sta", //
					"St Albert Ctr" //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Albert Ctr", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_A + 9L) { // A9
			if (Arrays.asList( //
					"Kingswood", //
					"Vlg Transit Sta" //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vlg Transit Sta", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_A + 14L) { // A14
			if (Arrays.asList( //
					"St Albert Ctr", //
					"Vlg Transit Sta" //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vlg Transit Sta", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					ST_ALBERT + " Ctr", //
					ST_ALBERT + " North" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_ALBERT + " North", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_A + 33L) { // A33
			if (Arrays.asList( //
					"Pineview", //
					"Naki TC" //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Naki TC", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == RID_RR) {
			if (Arrays.asList( //
					"Rodeo (Parade Detour)", //
					"Rodeo" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Rodeo", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 201L) {
			if (Arrays.asList( //
					"St Albert", //
					"Edmonton" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Edmonton", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"MacEwan University", //
					"Edmonton" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Edmonton", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 202L) {
			if (Arrays.asList( //
					VILLAGE_TRANSIT_STATION, //
					"St Albert Ctr Exch" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Albert Ctr Exch", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"2", // TODO?
					ST_ALBERT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_ALBERT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 203L) {
			if (Arrays.asList( //
					"Kingsway " + TRANSIT_CENTRE_SHORT, //
					"St Albert" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Albert", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Westmount " + TRANSIT_CENTRE_SHORT, //
					"Kingsway " + TRANSIT_CENTRE_SHORT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Kingsway " + TRANSIT_CENTRE_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 208L) {
			if (Arrays.asList( //
					"Gov Ctr", // <>
					"St Albert" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Albert", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final Pattern EXCHANGE_ = Pattern.compile("(exchange)", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = EXCHANGE;

	private static final Pattern GOVERNMENT_CENTRE = Pattern.compile("(government (centre|center))", Pattern.CASE_INSENSITIVE);
	private static final String GOVERNMENT_CENTRE_REPLACEMENT = GOV_CTR;

	private static final Pattern ENDS_WITH_EXPRESS_ = Pattern.compile("( express$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_EXPRESS_REPLACEMENT = StringUtils.EMPTY;

	private static final Pattern TRANSIT_CENTRE_ = Pattern.compile("((^|\\W){1}(transit centre|transit center|tc)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTRE_REPLACEMENT = "$2" + TRANSIT_CENTRE_SHORT + "$4";

	private static final Pattern UNIVERSITY_OF_ALBERTA = Pattern.compile("(university Of alberta)", Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_ALBERTA_REPLACEMENT = U_OF_ALBERTA;

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = CleanUtils.keepTo(tripHeadsign);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = EXCHANGE_.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = GOVERNMENT_CENTRE.matcher(tripHeadsign).replaceAll(GOVERNMENT_CENTRE_REPLACEMENT);
		tripHeadsign = TRANSIT_CENTRE_.matcher(tripHeadsign).replaceAll(TRANSIT_CENTRE_REPLACEMENT);
		tripHeadsign = UNIVERSITY_OF_ALBERTA.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_ALBERTA_REPLACEMENT);
		tripHeadsign = ENDS_WITH_EXPRESS_.matcher(tripHeadsign).replaceAll(ENDS_WITH_EXPRESS_REPLACEMENT);
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

	private static final Pattern STARTS_WITH_STOP_CODE = Pattern.compile("(" //
			+ "^[0-9]{4,5}[\\s]*\\-[\\s]*" //
			+ "|" //
			+ "^[A-Z]{1}[\\s]*\\-[\\s]*" //
			+ ")", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = STARTS_WITH_STOP_CODE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
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

	@Override
	public String getStopCode(GStop gStop) {
		if (StringUtils.isEmpty(gStop.getStopCode()) || "0".equals(gStop.getStopCode())) {
			return String.valueOf(gStop.getStopId());
		}
		return super.getStopCode(gStop);
	}

	@Override
	public int getStopId(GStop gStop) {
		if (Utils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
		}
		if (!Utils.isDigitsOnly(gStop.getStopId())) {
			if ("A".equals(gStop.getStopId())) {
				return 10000;
			} else if ("B".equals(gStop.getStopId())) {
				return 20000;
			} else if ("C".equals(gStop.getStopId())) {
				return 30000;
			} else if ("D".equals(gStop.getStopId())) {
				return 40000;
			} else if ("E".equals(gStop.getStopId())) {
				return 50000;
			} else if ("F".equals(gStop.getStopId())) {
				return 60000;
			} else if ("G".equals(gStop.getStopId())) {
				return 70000;
			} else if ("H".equals(gStop.getStopId())) {
				return 80000;
			} else if ("I".equals(gStop.getStopId())) {
				return 90000;
			} else if ("J".equals(gStop.getStopId())) {
				return 100000;
			} else if ("K".equals(gStop.getStopId())) {
				return 110000;
			}
			throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
		}
		return super.getStopId(gStop);
	}
}
