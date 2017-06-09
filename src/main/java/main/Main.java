package main;

import command.CommandListener;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;

import static util.Util.createClient;
import static util.Util.readToken;

public class Main {
    private final String token;
    private final IDiscordClient bot;
    private final EventDispatcher dispatcher;

    public Main() {
        token = readToken();
        bot = createClient(token);
        dispatcher = bot.getDispatcher(); //throws an NPE if bot is null
        dispatcher.registerListener(new CommandListener(bot)); //throws an NPE if dispatcher is null
    }

    public static void main(String[] args) {
        new Main();
    }
}
