package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.List;

import static util.Util.buildAndSendMessage;

public class RockPaperScissorsCommand extends Command {
    public enum State {
        ROCK, PAPER, SCISSORS
    }

    private final List<String> argsList;

    public RockPaperScissorsCommand(IDiscordClient bot, MessageReceivedEvent event, List<String> argsList) {
        super(bot, event, new Permission(Permission.Value.ALL));
        this.argsList = argsList;
    }

    @Override
    public void execute() {
        //If no arguments are passed in.
        if (argsList.isEmpty()) {
            buildAndSendMessage(bot, event.getAuthor().mention() + " Please include a valid argument of which side you take!", event.getChannel());
            return;
        }

        //If too many arguments are passed in.
        if (argsList.size() > 1) {
            buildAndSendMessage(bot, event.getAuthor().mention() + " One argument only!", event.getChannel());
            return;
        }

        //Bot State
        int state = (int)(Math.random() * 3); //0, 1, 2
        State botState = null;
        switch (state) {
            case 0:
                botState = State.ROCK;
                break;
            case 1:
                botState = State.PAPER;
                break;
            case 2:
                botState = State.SCISSORS;
                break;
        }

        //User state
        String userGet = argsList.get(0).toLowerCase();
        State userState = null;
        switch (userGet) {
            case "rock":
                userState = State.ROCK;
                break;
            case "paper":
                userState = State.PAPER;
                break;
            case "scissors":
                userState = State.SCISSORS;
                break;
            default:
                buildAndSendMessage(bot, event.getAuthor().mention() + " Please include a valid argument of which side you take!", event.getChannel());
                return;
        }

        //Comparison & Result
        String result = event.getAuthor().mention() + " ";
        if (userState == botState) {
            result += "Tie!";
        } else if (userState == State.ROCK) {
            if (botState == State.PAPER) {
                result += "You lose!";
            } else {
                result += "You win!";
            }
        } else if (userState == State.PAPER) {
            if (botState == State.SCISSORS) {
                result += "You lose!";
            } else {
                result += "You win!";
            }
        } else {
            if (botState == State.ROCK) {
                result += "You lose!";
            } else {
                result += "You win!";
            }
        }
        result += "\tBot: " + botState + "\tUser: "  + userState;
        buildAndSendMessage(bot, result, event.getChannel());
    }
}
