package main;

import command.CommandListener;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;

import java.io.*;

import static util.Util.createClient;

public class Main {
    private final String token;
    private final IDiscordClient bot;
    private final EventDispatcher dispatcher;

    public Main() {
        token = readFromPropeties();
        bot = createClient(token);
        dispatcher = bot.getDispatcher(); //throws an NPE if bot is null
        dispatcher.registerListener(new CommandListener(bot)); //throws an NPE if dispatcher is null
    }

    private String readFromPropeties() {
        File file = new File("properties/creds.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String result = null;
        try {
            result = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String[] args) {
        new Main();
    }
}
