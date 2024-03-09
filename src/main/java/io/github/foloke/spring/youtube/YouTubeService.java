package io.github.foloke.spring.youtube;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Builder;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import io.github.foloke.spring.services.localization.BotLocalization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * YT client holder
 *
 * @author Марченко Дмитрий
 * @since 09.03.2024
 */
@Service
public class YouTubeService {
	private static final long PAGE_RESULTS = 10L;
	private static final String SEARCH_PARAMS = "id,snippet";
	private static final String VIDEO_TYPE = "video";
	private static final String APPLICATION_NAME = "HueBot";
	private final BotLocalization playerLocalization;
	@Value("${ytApiKey}")
	private String ytApiKey;
	private YouTube youTube;

	@Autowired
	private YouTubeService(BotLocalization playerLocalization) {
		this.playerLocalization = playerLocalization;
	}

	/**
	 * Searches YT videos by passed query
	 */
	public List<SearchResult> serachForVideo(String searchQueryString) throws IOException {
		Search.List searchQuery = getClient().search().list(SEARCH_PARAMS);
		searchQuery.setMaxResults(PAGE_RESULTS);
		searchQuery.setType(VIDEO_TYPE);
		searchQuery.setQ(searchQueryString);
		SearchListResponse searchResponse = searchQuery.execute();
		return searchResponse.getItems();
	}


	private YouTube getClient() {
		if (youTube == null) {
			if (ytApiKey.isEmpty()) {
				throw new NoApiKeyException(playerLocalization.getMessage("no_yt_api_key"));
			}
			youTube = new Builder(new NetHttpTransport(), new JacksonFactory(), request -> { })
				.setApplicationName(APPLICATION_NAME)
				.setYouTubeRequestInitializer(new YouTubeRequestInitializer(ytApiKey))
				.build();
		}
		return youTube;
	}

}
