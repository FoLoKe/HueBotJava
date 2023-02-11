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
	prev {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.prev();
		}

		@Override
		public String getText() {
			return "⏮";
		}
	},
	play {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.play();
		}

		@Override
		public String getText() {
			return "⏯";
		}
	},
	skip {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.skip();
		}

		@Override
		public String getText() {
			return "⏭";
		}
	},
	shuffle {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.shuffle();
		}

		@Override
		public String getText() {
			return "\uD83D\uDD00";
		}
	},
	repeatQ {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.repeatQ();
		}

		@Override
		public String getText() {
			return "🔁";
		}
	},
	repeat {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.repeat();
		}

		@Override
		public String getText() {
			return "🔄";
		}
	},

	link {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.getLink();
		}

		@Override
		public String getText() {
			return "↗";
		}
	},
	unload {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.unload();
		}

		@Override
		public String getText() {
			return "⏏";
		}
	},
	minus {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.addVolume(-5);
		}

		@Override
		public String getText() {
			return "🔉";
		}
	},
	plus {
		@Override
		void execute(BotPlayer botPlayer) {
			botPlayer.addVolume(5);
		}

		@Override
		public String getText() {
			return "🔊";
		}
	};
	abstract void execute(BotPlayer botPlayer);

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
	public static void runCommand(ButtonInteractionEvent event, BotPlayer botPlayer) {
		Arrays.stream(values())
			.filter(command -> event.getCustomId().equals(command.name()))
			.findFirst()
			.ifPresent(command -> {
				command.execute(botPlayer);
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
