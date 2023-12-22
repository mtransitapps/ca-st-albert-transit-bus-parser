package org.mtransit.parser.ca_st_albert_transit_bus;

import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Schedules-zipped-files/urjq-fvmq
public class StAlbertTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new StAlbertTransitBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
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

	@Nullable
	@Override
	public String getAgencyId() {
		return "2"; // St. Albert Transit
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public @NotNull String getRouteShortName(@NotNull GRoute gRoute) {
		if (FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
			return super.getRouteShortName(gRoute);
		}
		//noinspection deprecation
		return gRoute.getRouteId(); // used by GTFS-RT
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
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
			+ "^[0-9]{4,5}\\s*-\\s*" //
			+ "|" //
			+ "^[A-Z]\\s*-\\s*" //
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
		if (FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
			if (StringUtils.isEmpty(gStop.getStopCode())
					|| "0".equals(gStop.getStopCode())) {
				//noinspection deprecation
				return gStop.getStopId(); // used by GTFS-RT
			}
			return super.getStopCode(gStop);
		}
		//noinspection deprecation
		return gStop.getStopId(); // used by GTFS-RT
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
