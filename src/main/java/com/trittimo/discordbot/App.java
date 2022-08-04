package com.trittimo.discordbot;

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
            App.run(properties);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
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