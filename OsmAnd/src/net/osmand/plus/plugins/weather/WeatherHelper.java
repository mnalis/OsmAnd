package net.osmand.plus.plugins.weather;

import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.jni.BandIndexGeoBandSettingsHash;
import net.osmand.core.jni.GeoBandSettings;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.ZoomLevelDoubleListHash;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WeatherHelper {

	private static final String WEATHER_FORECAST_DOWNLOAD_STATE_PREFIX = "forecast_download_state_";
	private static final String WEATHER_FORECAST_LAST_UPDATE_PREFIX = "forecast_last_update_";
	private static final String WEATHER_FORECAST_FREQUENCY_PREFIX = "forecast_frequency_";
	private static final String WEATHER_FORECAST_TILE_IDS_PREFIX = "forecast_tile_ids_";
	private static final String WEATHER_FORECAST_WIFI_PREFIX = "forecast_download_via_wifi_";

	private static final int TILE_SIZE = 40000;
	private static final int FORECAST_DATES_COUNT = 24 + (6 * 8) + 1;
	private static final int WEATHER_FORECAST_FREQUENCY_HALF_DAY = 43200;
	private static final int WEATHER_FORECAST_FREQUENCY_DAY = 86400;
	private static final int WEATHER_FORECAST_FREQUENCY_WEEK = 604800;

	private final OsmandApplication app;
	private final WeatherSettings weatherSettings;

	private final Map<Short, WeatherBand> weatherBands = new LinkedHashMap<>();
	private final AtomicInteger bandsSettingsVersion = new AtomicInteger(0);

	private static final Log log = PlatformUtil.getLog(WeatherHelper.class);

	private WeatherTileResourcesManager weatherTileResourcesManager;

	public WeatherHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.weatherSettings = new WeatherSettings(app);

		weatherBands.put(WEATHER_BAND_TEMPERATURE, WeatherBand.withWeatherBand(app, WEATHER_BAND_TEMPERATURE));
		weatherBands.put(WEATHER_BAND_PRESSURE, WeatherBand.withWeatherBand(app, WEATHER_BAND_PRESSURE));
		weatherBands.put(WEATHER_BAND_WIND_SPEED, WeatherBand.withWeatherBand(app, WEATHER_BAND_WIND_SPEED));
		weatherBands.put(WEATHER_BAND_CLOUD, WeatherBand.withWeatherBand(app, WEATHER_BAND_CLOUD));
		weatherBands.put(WEATHER_BAND_PRECIPITATION, WeatherBand.withWeatherBand(app, WEATHER_BAND_PRECIPITATION));
	}

	@NonNull
	public WeatherSettings getWeatherSettings() {
		return weatherSettings;
	}

	@NonNull
	public List<WeatherBand> getWeatherBands() {
		return new ArrayList<>(weatherBands.values());
	}

	public boolean hasVisibleBands() {
		return !Algorithms.isEmpty(getVisibleBands());
	}

	@NonNull
	public List<WeatherBand> getVisibleBands() {
		List<WeatherBand> bands = new ArrayList<>();
		for (WeatherBand band : weatherBands.values()) {
			if (band.isBandVisible()) {
				bands.add(band);
			}
		}
		return bands;
	}

	@NonNull
	public List<WeatherBand> getVisibleForecastBands() {
		List<WeatherBand> bands = new ArrayList<>();
		for (WeatherBand band : weatherBands.values()) {
			if (band.isForecastBandVisible()) {
				bands.add(band);
			}
		}
		return bands;
	}

	@Nullable
	public WeatherBand getWeatherBand(short bandIndex) {
		return weatherBands.get(bandIndex);
	}


	public void updateMapPresentationEnvironment(MapRendererContext mapRendererContext) {
		if (weatherTileResourcesManager != null) {
			return;
		}
		File weatherForecastDir = app.getAppPath(WEATHER_FORECAST_DIR);
		if (!weatherForecastDir.exists()) {
			weatherForecastDir.mkdir();
			SimpleDateFormat frmt = new SimpleDateFormat("yyyyMMdd");
			long cleanup = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2);
			for (File f : weatherForecastDir.listFiles()) {
				String fileName = f.getName();
				if (fileName.endsWith(".db") && fileName.length() > 8) {
					try {
						Date dt = frmt.parse(fileName.substring(0, 8));
						if (dt.getTime() < cleanup) {
							f.delete();
						}
					} catch (RuntimeException | ParseException e) {
						log.error(String.format("Unexpected file name in weather folder %s", fileName));
					}
				}
			}
		}
		String projResourcesPath = app.getAppPath(null).getAbsolutePath();
		int tileSize = 256;
		MapPresentationEnvironment mapPresentationEnvironment = mapRendererContext.getMapPresentationEnvironment();
		float densityFactor = mapPresentationEnvironment.getDisplayDensityFactor();

		WeatherWebClient webClient = new WeatherWebClient();
		WeatherTileResourcesManager weatherTileResourcesManager = new WeatherTileResourcesManager(new BandIndexGeoBandSettingsHash(),
				weatherForecastDir.getAbsolutePath(), projResourcesPath, tileSize, densityFactor, webClient.instantiateProxy(true));
		webClient.swigReleaseOwnership();
		weatherTileResourcesManager.setBandSettings(getBandSettings(weatherTileResourcesManager));
		this.weatherTileResourcesManager = weatherTileResourcesManager;
	}


	@Nullable
	public WeatherTileResourcesManager getWeatherResourcesManager() {
		return weatherTileResourcesManager;
	}

	public boolean updateBandsSettings() {
		WeatherTileResourcesManager weatherResourcesManager = getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return false;
		}
		return updateBandsSettings(weatherResourcesManager);
	}

	private boolean updateBandsSettings(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		BandIndexGeoBandSettingsHash bandSettings = getBandSettings(weatherResourcesManager);
		weatherResourcesManager.setBandSettings(bandSettings);
		bandsSettingsVersion.incrementAndGet();
		return true;
	}

	@NonNull
	public BandIndexGeoBandSettingsHash getBandSettings(@NonNull WeatherTileResourcesManager weatherResourcesManager) {
		BandIndexGeoBandSettingsHash bandSettings = new BandIndexGeoBandSettingsHash();

		for (WeatherBand band : weatherBands.values()) {
			WeatherUnit weatherUnit = band.getBandUnit();
			if (weatherUnit != null) {
				String unit = weatherUnit.getSymbol();
				String unitFormatGeneral = band.getBandGeneralUnitFormat();
				String unitFormatPrecise = band.getBandPreciseUnitFormat();
				String internalUnit = band.getInternalBandUnit();
				float opacity = band.getBandOpacity();
				String contourStyleName = band.getContourStyleName();
				String colorProfilePath = app.getAppPath(band.getColorFilePath()).getAbsolutePath();
				MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
				ZoomLevelDoubleListHash contourLevels = band.getContourLevels(weatherResourcesManager,
						mapContext != null? mapContext.getMapPresentationEnvironment() : null);
				GeoBandSettings settings = new GeoBandSettings(unit, unitFormatGeneral, unitFormatPrecise,
						internalUnit, opacity, colorProfilePath, contourStyleName, contourLevels);
				bandSettings.set(band.getBandIndex(), settings);
			}
		}
		return bandSettings;
	}

	public int getBandsSettingsVersion() {
		return bandsSettingsVersion.get();
	}

	public static long roundForecastTimeToHour(long time) {
		long hour = 60 * 60 * 1000;
		return (time + hour / 2) / hour * hour;
	}
}
