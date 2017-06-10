package main;

import command.CommandListener;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;

import java.io.*;

public class Main {
    public Main() {
        String token = readToken();
        IDiscordClient bot = createClient(token);
        EventDispatcher dispatcher = bot.getDispatcher();
        dispatcher.registerListener(new CommandListener(bot)); //throws an NPE if dispatcher is null
    }

    /*
        Creates an instance of the bot.
    */
    private IDiscordClient createClient(String token) {
        ClientBuilder clientBuilderWithToken = new ClientBuilder().withToken(token);
        try {
            return clientBuilderWithToken.login();
        } catch (DiscordException e) {
            e.printStackTrace();
            return null; //will throw an exception further down the line, when the program tries to access the client
        }
    }

    /*
        Reads the token from the credentials files.
    */
    private String readToken() {
        File file = new File("properties/token.txt");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        }

        String result;
        try {
            result = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
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
