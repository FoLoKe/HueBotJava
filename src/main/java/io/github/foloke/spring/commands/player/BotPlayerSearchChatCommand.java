package io.github.foloke.spring.commands.player;

import com.google.api.services.youtube.model.SearchResult;
import com.google.common.collect.Lists;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.spring.youtube.NoApiKeyException;
import io.github.foloke.spring.youtube.YouTubeService;
import io.github.foloke.utils.commands.BotChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Streams.mapWithIndex;
import static io.github.foloke.utils.commands.BotButtonCommand.ID_PARAMS_SEPARATOR;

/**
 * YT tracks search
 *
 * @author Марченко Дмитрий
 * @since 08.03.2024
 */
@Component
@Qualifier("local")
public class BotPlayerSearchChatCommand implements BotChatCommand {
	private static final String SEARCH_QUERY_PARAM = "query";
	private static final String BASE_YT_VIDEO_URL = "https://www.youtube.com/watch?v=";
	private static final String LINK_FORMAT = "%d - [%s](%s%s)";

	private final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());

	private final BotLocalization playerLocalization;
	private final YouTubeService youTubeService;

	@Autowired
	private BotPlayerSearchChatCommand(BotLocalization playerLocalization, YouTubeService youTubeService) {
		this.playerLocalization = playerLocalization;
		this.youTubeService = youTubeService;
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}

	@Override
	public String getDescription() {
		return playerLocalization.getMessage("serach_command_description");
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.singletonList(ApplicationCommandOptionData.builder()
			.name(SEARCH_QUERY_PARAM)
			.description(playerLocalization.getMessage("track_name"))
			.type(Type.STRING.getValue())
			.required(true)
			.build()
		);
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
		String searchQueryString = event.getOption(SEARCH_QUERY_PARAM)
			.flatMap(ApplicationCommandInteractionOption::getValue)
			.map(ApplicationCommandInteractionOptionValue::asString)
			.orElse("");
		try {
			List<SearchResult> searchResultList = youTubeService.serachForVideo(searchQueryString);
			if (searchResultList != null) {
				event.editReply(InteractionReplyEditSpec.builder()
					.addEmbed(EmbedCreateSpec.create()
						.withFields(getFieldList(searchResultList))
						.withTitle(playerLocalization.getMessage("tracks_found_caption"))
					).components(createButtons(searchResultList)).build()
				).block();
				return;
			}

		} catch (IOException e) {
			log.error("error on track search", e);
			event.editReply(playerLocalization.getMessage("search_error")).block();
			return;
		} catch (NoApiKeyException e) {
			log.warn("No API in .env file", e);
			event.editReply(e.getMessage()).block();
			return;
		}
		event.editReply(playerLocalization.getMessage("no_search_result")).block();
	}

	private List<Field> getFieldList(List<SearchResult> searchResultList) {
		return mapWithIndex(searchResultList.stream(), BotPlayerSearchChatCommand::createField)
			.collect(Collectors.toList());
	}

	private static Field createField(SearchResult searchResult, long index) {
		return Field.of(
			"",
			String.format(
				LINK_FORMAT,
				index + 1,
				searchResult.getSnippet().getTitle(),
				BASE_YT_VIDEO_URL,
				searchResult.getId().getVideoId()
			),
			false
		);
	}

	private List<LayoutComponent> createButtons(List<SearchResult> searchResultList) {
		List<String> idList = searchResultList.stream()
			.map(searchResult -> searchResult.getId().getVideoId())
			.collect(Collectors.toList());
		List<LayoutComponent> layoutComponentList = new ArrayList<>();
		Lists.partition(
			mapWithIndex(idList.stream(), BotPlayerSearchChatCommand::createButton).collect(Collectors.toList()),
			5
		).stream()
			.map(ActionRow::of)
			.forEach(layoutComponentList::add);
		return layoutComponentList;
	}

	private static Button createButton(String trackId, long index) {
		return Button.success(BotChooseTrackSearchResultButtonCommand.COMMNAD_NAME
			+ ID_PARAMS_SEPARATOR
			+ trackId
			+ ID_PARAMS_SEPARATOR
			+ index,
			Long.toString(index + 1)
		);
	}

	@Override
	public String getCommandName() {
		return "search";
	}
}
