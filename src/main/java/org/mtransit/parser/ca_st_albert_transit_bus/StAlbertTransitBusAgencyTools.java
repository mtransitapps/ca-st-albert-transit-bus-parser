package org.mtransit.parser.ca_st_albert_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// https://stalbert.ca/city/transit/tools/open-data-gtfs/
// https://gtfs.edmonton.ca/TMGTFSRealTimeWebService/GTFS/GTFS.zip
public class StAlbertTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new StAlbertTransitBusAgencyTools().start(args);
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "St AT";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	private static final int AGENCY_ID_INT = GIDs.getInt("2"); // St. Albert Transit

	@Override
	public boolean excludeAgency(@NotNull GAgency gAgency) {
		if (gAgency.getAgencyIdInt() != AGENCY_ID_INT) {
			return EXCLUDE;
		}
		return super.excludeAgency(gAgency);
	}

	@NotNull
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
	private static final long RID_N = 14_000L;
	private static final long RID_R = 18_000L;
	private static final long RID_S = 19_000L;
	private static final long RID_FA = 600_000L + RID_A;
	private static final long RID_RA = 1_800_000L + RID_A;
	private static final long RID_RR = 1_800_000L + RID_R;
	private static final long RID_SN = 1_900_000L + RID_N;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			if (SNOWFLAKE_FESTIVAL_SHUTTLE.equals(gRoute.getRouteLongName())) {
				return RID_SN;
			}
			if (FIRE_AND_ICE_FESTIVAL.equals(gRoute.getRouteLongName())) {
				return RID_FA;
			}
			throw new MTLog.Fatal("Unexpected route ID %s!", gRoute.toStringPlus());
		}
		if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
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
		final Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			final long id = Long.parseLong(matcher.group());
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

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
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

	private static final String EDMONTON = "Edm";
	private static final String GOV_CTR = "Gov Ctr";
	private static final String ST_ALBERT = "St Albert";
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
	private static final String SNOWFLAKE_FESTIVAL_SHUTTLE = "Snowflake Festival Shuttle";
	private static final String FIRE_AND_ICE_FESTIVAL = "Fire and Ice Festival";

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

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			if (!CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
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

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final Pattern STARTS_WITH_A_ = Pattern.compile("(^A)", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_A_REPLACEMENT = EMPTY;

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = STARTS_WITH_A_.matcher(gStopId).replaceAll(STARTS_WITH_A_REPLACEMENT);
		return gStopId;
	}

	@Override
	public boolean directionSplitterEnabled() {
		return true; // ALLOWED
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		if (routeId == 21L + RID_A) {
			return true; // ENABLED
		}
		return false; // DISABLED
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_EXPRESS_ = Pattern.compile("( express$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_EXPRESS_REPLACEMENT = EMPTY;

	private static final Pattern EDMONTON_ = Pattern.compile("((\\w+) edmonton)", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = CleanUtils.cleanWordsReplacement("$2 Edm");

	private static final Pattern STATS_WITH_ST_ALBERT_CTR_ = Pattern.compile("(^(st albert center|st albert centre|st albert ctr) )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = ENDS_WITH_EXPRESS_.matcher(tripHeadsign).replaceAll(ENDS_WITH_EXPRESS_REPLACEMENT);
		tripHeadsign = EDMONTON_.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = STATS_WITH_ST_ALBERT_CTR_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.SAINT.matcher(tripHeadsign).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{
				"TC",
		};
	}

	private static final Pattern STARTS_WITH_STOP_CODE = Pattern.compile("(" //
			+ "^[0-9]{4,5}[\\s]*-[\\s]*" //
			+ "|" //
			+ "^[A-Z][\\s]*-[\\s]*" //
			+ ")", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = STARTS_WITH_STOP_CODE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = EDMONTON_.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.SAINT.matcher(gStopName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if (StringUtils.isEmpty(gStop.getStopCode())
				|| "0".equals(gStop.getStopCode())) {
			//noinspection deprecation
			return gStop.getStopId();
		}
		return super.getStopCode(gStop);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		if (CharUtils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
		}
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (!CharUtils.isDigitsOnly(stopId)) {
			switch (stopId) {
			case "A":
				return 10_000;
			case "B":
				return 20_000;
			case "C":
				return 30_000;
			case "D":
				return 40_000;
			case "E":
				return 50_000;
			case "F":
				return 60_000;
			case "G":
				return 70_000;
			case "H":
				return 80_000;
			case "I":
				return 90_000;
			case "J":
				return 100_000;
			case "K":
				return 110_000;
			}
			throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
		}
		return super.getStopId(gStop);
	}
}
