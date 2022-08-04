package com.trittimo.discordbot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class App {
    public static final String KEY_VAULT_NAME = System.getenv("KEY_VAULT_NAME");

    public static void main(String[] args) {
        String keyVaultUri = "https://" + KEY_VAULT_NAME + ".vault.azure.net";

        try {
            SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

            KeyVaultSecret discordTokenSecret = secretClient.getSecret("discord-token");
            Properties properties = new Properties();
            properties.setProperty("DISCORD_TOKEN", discordTokenSecret.getValue());
            App.startWebListener();
            App.run(properties);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void startWebListener() {
        Runnable webListener = new Runnable() {
            public void run() {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
                    server.createContext("/", new HttpHandler() {
                        public void handle(HttpExchange t) {
                            System.out.println("Somebody attempted to access port 80 with a GET request");
                            String response = "This keeps the app running!";
                            try {
                                t.sendResponseHeaders(200, response.length());
                                OutputStream s = t.getResponseBody();
                                s.write(response.getBytes());
                                s.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            
                        }
                    });
                    server.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(webListener);
        thread.start();
    }

    public static void run(Properties properties) {
        String discordToken = properties.getProperty("DISCORD_TOKEN");
        if (discordToken == null) {
            throw new RuntimeException("Unable to get Discord token");
        }
        DiscordClient.create(discordToken).withGateway(client -> client.on(MessageCreateEvent.class, event -> {
            Message message = event.getMessage();
            if (message.getContent().equalsIgnoreCase("!ping")) {
                return message.getChannel().flatMap(channel -> channel.createMessage("Pong!"));
            }
            return Mono.empty();
        })).block();
    }
}