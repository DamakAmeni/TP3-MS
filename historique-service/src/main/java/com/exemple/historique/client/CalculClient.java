package com.exemple.historique.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CalculClient {

    private final RestClient client;

    // @Value lit la propriété calcul.service.url de application.properties
    public CalculClient(@Value("${calcul.service.url}") String url) {
        this.client = RestClient.create(url);
    }

    // Envoie GET {url}/calcul/{operateur}?a=..&b=.. et retourne le résultat.
    public double calculer(String operateur, int a, int b) {
        String corps = client.get()
                .uri("/calcul/{op}?a={a}&b={b}", operateur, a, b)
                .retrieve()
                .body(String.class);
        return Double.parseDouble(corps);
    }
}