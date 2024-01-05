package io.github.foloke.player;

import com.google.common.collect.Lists;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Player connads
 *
 * @author Dmitry Marchenko
 * @since 11.02.2023
 */
public enum BotPlayerButtonControls {
	PREVIOUS {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.playPrevious();
		}

		@Override
		public String getText() {
			return "⏮";
		}
	},
	PLAY {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.play();
		}

		@Override
		public String getText() {
			return "⏯";
		}
	},
	SKIP {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.skip();
		}

		@Override
		public String getText() {
			return "⏭";
		}
	},
	SHUFFLE {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.shuffle();
		}

		@Override
		public String getText() {
			return "\uD83D\uDD00";
		}
	},
	REPEAT_QUEUE {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.toggleRepeatQueue();
		}

		@Override
		public String getText() {
			return "🔁";
		}
	},
	REPEAT_TRACK {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.toggleRepeatTrack();
		}

		@Override
		public String getText() {
			return "🔄";
		}
	},

	GET_LINK {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.getLink();
		}

		@Override
		public String getText() {
			return "↗";
		}
	},
	UNLOAD_QUEUE {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.unload();
		}

		@Override
		public String getText() {
			return "⏏";
		}
	},
	REDUCE_VOLUME {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.reduceVolume();
		}

		@Override
		public String getText() {
			return "🔉";
		}
	},
	ADD_VOLUME {
		@Override
		void execute(BotGuildPlayer botGuildPlayer) {
			botGuildPlayer.addVolume();
		}

		@Override
		public String getText() {
			return "🔊";
		}
	};
	abstract void execute(BotGuildPlayer botGuildPlayer);

	public Button getButton() {
		return Button.secondary(name(), ReactionEmoji.unicode(getText()));
	}

	public String getText() {
		return "Button";
	}

	/**
	 * Post interaction reply
	 */
	public void reply(ButtonInteractionEvent event) {
		event.edit(InteractionApplicationCommandCallbackSpec.builder().build()).block();
	}

	/**
	 * Runs player command if found
	 */
	public static void runCommand(ButtonInteractionEvent event, BotGuildPlayer botGuildPlayer) {
		Arrays.stream(values())
			.filter(command -> event.getCustomId().equals(command.name()))
			.findFirst()
			.ifPresent(command -> {
				command.execute(botGuildPlayer);
				command.reply(event);
			});
	}

	public static List<LayoutComponent> getButtons() {
		List<LayoutComponent> layoutComponentList = new ArrayList<>();
		Lists.partition(Arrays.stream(values()).map(BotPlayerButtonControls::getButton).collect(Collectors.toList()), 5)
			.stream()
			.map(ActionRow::of)
			.forEach(layoutComponentList::add);
		return layoutComponentList;
	}
}
