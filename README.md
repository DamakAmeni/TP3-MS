# TP3 REST : Microservices « Calculatrice » avec Spring Boot

**Matière :** Architecture MicroServices, **Filière :** MPSRCC1

---

## Partie A : Du service web au microservice

### A.4 Questions de conception

**1. Avantage et inconvénient d'un calcul obtenu par requête HTTP plutôt que par appel de méthode.**

- *Avantage* : les deux services sont indépendants. On peut les développer, les démarrer, les arrêter, les redéployer et même les faire évoluer séparément, sans toucher à l'autre.
- *Inconvénient* : l'appel passe par le réseau. Il est plus lent qu'un appel de méthode, et il peut échouer (service arrêté, réseau coupé). Il faut donc gérer ces erreurs.

**2. Si `calcul-service` est arrêté, quelles requêtes du tableau A.2 continuent de fonctionner ?**

- *Continuent de fonctionner* : `GET /operations`, `GET /operations/{id}` et `DELETE /operations/{id}`, car elles n'utilisent que la mémoire de `historique-service`.
- *Échouent* : les quatre requêtes `GET :8081/calcul/...` (le service n'écoute plus), ainsi que `POST /operations` et `PUT /operations/{id}`, qui ont besoin d'un calcul (réponse 503). Un `PUT` sur un identifiant inexistant répond quand même 404, car l'existence est vérifiée avant l'appel à `calcul-service`.

**3. Pourquoi les deux microservices ne peuvent-ils pas écouter sur le même port ?**

Un port ne peut être occupé que par un seul programme à la fois. Si un second processus essaie d'écouter sur le même port, son démarrage échoue (« Port already in use »). Chaque microservice a donc besoin de son propre port (8081 et 8082).

**4. Quel code HTTP `historique-service` renvoie-t-il quand il ne peut pas joindre `calcul-service` ?**

Le code **503 (Service Unavailable)** : le serveur ne peut pas répondre pour l'instant parce qu'un service dont il dépend est indisponible. Ce n'est ni une erreur du client (4xx), ni un bug du serveur (500).

---

## Partie C : Premier microservice : la somme

### C.2 Correspondance avec le TP2

| Spring (TP3) | JAX-RS (TP2) | Rôle dans cet exemple |
|---|---|---|
| `@RestController` | (pas d'équivalent direct) | Déclare la classe comme contrôleur REST : Spring la détecte, crée un objet et convertit les valeurs retournées en corps de réponse |
| `@RequestMapping("/calcul")` | `@Path("/calcul")` sur la classe | Donne l'URI de base de toutes les méthodes de la classe |
| `@GetMapping("/somme")` | `@GET` + `@Path("/somme")` | La méthode répond aux requêtes GET sur `/calcul/somme` |
| `produces = MediaType.TEXT_PLAIN_VALUE` | `@Produces(MediaType.TEXT_PLAIN)` | La réponse est du texte brut (`text/plain`) |
| `@RequestParam("a") int a` | `@QueryParam("a") int a` | Lit le paramètre `a` de la chaîne de requête (`?a=2`) |
| `ResponseEntity.ok(...)` | `Response.ok(...).build()` | Construit une réponse 200 avec son corps |

### C.6 Comprendre l'URL du service

| Partie de l'URL | Origine |
|---|---|
| `localhost:8081` | Machine locale et port fixé par `server.port` dans `application.properties` |
| `/calcul` | Annotation `@RequestMapping("/calcul")` de la classe |
| `/somme` | Annotation `@GetMapping("/somme")` de la méthode |
| `?a=2&b=3` | Paramètres de requête, lus par `@RequestParam("a")` et `@RequestParam("b")` |

### Question C.1

TP2 : `http://localhost:8080/RestWebApp/api/calcul/somme?a=2&b=3`
TP3 : `http://localhost:8081/calcul/somme?a=2&b=3`

Ont disparu : **`/RestWebApp`** (nom de l'application) et **`/api`** (préfixe de `@ApplicationPath`). Au TP2, plusieurs applications `.war` étaient déployées dans un même Tomcat, qui distinguait chaque application par son nom. Ici, le microservice embarque son propre serveur et est la seule application : elle est servie à la racine `/`. Il n'y a plus de classe `RestApplication` avec `@ApplicationPath`. Le port change aussi : 8081 au lieu de 8080.

### Question C.2

Avec POST sur `/calcul/somme?a=2&b=3`, on obtient **405 Method Not Allowed**, car seule la méthode GET est prévue pour cette URI. C'est le même code qu'au TP2 ; seul le format du corps d'erreur diffère (ici, le JSON généré par Spring).

---

## Partie D : Compléter la calculatrice

### Question D.1

Quand le paramètre `b` est absent ou n'est pas un entier (`a=abc`), Spring répond **400 Bad Request** et la méthode n'est pas exécutée. Au TP2, JAX-RS donnait 0 à un entier absent et calculait quand même. L'attitude de Spring est la plus conforme à REST : le client est prévenu que sa requête est incorrecte (code 4xx), au lieu de recevoir un résultat faux sans avertissement.

### Question D.2

Le JSON d'erreur renvoyé par Spring contient quatre champs : `timestamp`, `status`, `error` et `path`.

### Question D.3

| Spring | JAX-RS |
|---|---|
| `@RequestParam` | `@QueryParam` |
| `@PathVariable` | `@PathParam` |

Pour lire l'identifiant dans `/operations/{id}`, on utilise **`@PathVariable`**.

---

## Partie E : Échanger des données en JSON

### Question E.1

Oui, les deux textes JSON sont équivalents pour un client : l'ordre des champs d'un objet JSON n'a pas de signification, le client lit chaque valeur par son nom. Cette fois, la conversion a été faite par **Jackson**, inclus dans `spring-boot-starter-webmvc`, au lieu de JSON-B au TP2.

### Question E.2

Spring déduit les formats que la méthode peut produire à partir du type de retour et des convertisseurs disponibles. Il n'y a que Jackson (JSON) : aucun convertisseur XML n'est présent, donc la méthode ne peut produire que du `application/json`. Quand le client demande `Accept: application/xml`, le serveur ne peut pas satisfaire la demande, d'où la réponse **406 Not Acceptable**, même si le code ne mentionne aucun format.

---

## Partie F : Second microservice : `historique-service`

### Question F.1

Les lectures (`GET /operations`, `GET /operations/{id}`) ne lisent que les données en mémoire de `historique-service` et n'appellent pas `calcul-service` : elles continuent donc de fonctionner quand `calcul-service` est arrêté. Au TP2, cette situation n'était pas possible : une seule application `.war` contenait tout, donc tout fonctionnait ou tout s'arrêtait en même temps.

### Question F.2

Sans le bloc `catch (ResourceAccessException e)`, l'exception n'est interceptée par personne : elle remonte jusqu'à Spring, qui renvoie au client une réponse **500 Internal Server Error** avec un JSON générique, et affiche une trace d'erreur dans la console. Un 500 laisse croire à un bug de `historique-service`. Avec le `catch`, le client reçoit un **503** accompagné d'un message clair (« calcul-service est injoignable »), qui indique que le problème vient d'un service dont on dépend.

---

## Questions de synthèse

### 1. Tableau A.3 : étape du TP où chaque différence a été observée

| Aspect | Étape du TP |
|---|---|
| Technologie (Jersey → Spring Boot) | B.3 (`pom.xml`, `spring-boot-starter-webmvc`) |
| Découpage (un `.war` → deux applications) | A.2, B.1 et F.1 (deux projets distincts) |
| Résultat de la construction (`.war` → `.jar` exécutable) | B.3 et C.5 (`mvn clean package`, `java -jar`) |
| Serveur (Tomcat externe → Tomcat embarqué) | C.5 (`Tomcat started on port 8081`) |
| Port (8080 partagé → un port par microservice) | C.4 (`server.port`) et F.1 |
| Adresse | C.6 et question C.1 |
| Activation (`@ApplicationPath` → `@SpringBootApplication`) | B.4 |
| Ressource REST (`@Path` → `@RestController` + `@RequestMapping`) | C.2 |
| Verbes HTTP | C.2, F.5, F.8 et F.9 |
| Paramètres | C.2, D.4 et F.5 (`@RequestBody`) |
| Réponse HTTP (`Response` → `ResponseEntity`) | C.2, D.2 et F.5 |
| Conversion JSON (JSON-B → Jackson) | E.1 et E.2 |
| Création des objets (`new` / `static` → `@Service` et injection) | C.1, C.2 et F.4 |
| Communication (appel de méthode → requête HTTP) | F.3 (`CalculClient`) et F.6 |

### 2. Correspondance complète entre les annotations JAX-RS (TP2) et Spring (TP3)

| JAX-RS (TP2) | Spring (TP3) |
|---|---|
| `@ApplicationPath` (classe `RestApplication`) | `@SpringBootApplication` (classe avec `main`) |
| `@Path` sur la classe | `@RestController` + `@RequestMapping` |
| `@Path` sur une méthode | valeur de `@GetMapping`, `@PostMapping`, etc. |
| `@GET`, `@POST`, `@PUT`, `@DELETE` | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` |
| `@QueryParam` | `@RequestParam` |
| `@PathParam` | `@PathVariable` |
| `@Consumes(APPLICATION_JSON)` + paramètre objet | `@RequestBody` |
| `@Produces` | `produces = ...` |
| `Response` | `ResponseEntity` |
| `Response.created(uri)` | `ResponseEntity.created(uri)` |
| `uriInfo.getAbsolutePathBuilder()` | `ServletUriComponentsBuilder.fromCurrentRequest()` |
| JSON-B | Jackson |

### 3. Construire et lancer les deux microservices sans IntelliJ

Dans un premier terminal :

```
cd calcul-service
mvn clean package
java -jar target/calcul-service-0.0.1-SNAPSHOT.jar
```

Dans un second terminal :

```
cd historique-service
mvn clean package
java -jar target/historique-service-0.0.1-SNAPSHOT.jar
```

Chaque `.jar` contient l'application et Tomcat : il n'y a rien à déployer. Pour lancer sans construire le `.jar`, on peut utiliser `mvn spring-boot:run` à la racine de chaque projet. Si Maven n'est pas installé, on peut remplacer `mvn` par `mvnw.cmd` (Windows) ou `./mvnw` (Linux, macOS).

### 4. Un microservice est-il un service web ? Un service web est-il toujours un microservice ?

- Un microservice **est** un service web : `calcul-service` et `historique-service` exposent une API REST accessible par HTTP.
- Un service web n'est **pas toujours** un microservice : `RestWebApp.war` du TP2 est un service web, mais c'est une seule application qui regroupe le calcul et l'historique, déployée dans un Tomcat installé à part. Elle n'est ni autonome ni indépendante : ce n'est pas un microservice.

### 5. Problème de l'adresse écrite dans `application.properties` avec trois exemplaires de `calcul-service`

L'adresse `http://localhost:8081` désigne une seule instance. Les deux autres exemplaires ne recevraient jamais de requêtes de `historique-service` : la charge n'est pas répartie. Si cette instance tombait, `historique-service` ne fonctionnerait plus, même si les deux autres tournaient. Il faudrait un mécanisme de découverte des instances et de répartition de charge (traité au TP4).

### 6. Pourquoi `historique-service` ne contredit pas le principe stateless

Le principe stateless interdit de mémoriser l'**état du client** (sa session, son contexte) entre deux requêtes : chaque requête contient toutes les informations nécessaires pour être traitée. La liste des opérations n'est pas l'état d'un client, mais l'état des **ressources** de l'application, partagé par tous les clients. Le serveur ne dépend d'aucune requête précédente d'un client pour en traiter une nouvelle. Dans un vrai système, ces données seraient stockées dans une base de données.

### 7. Répétition de `POST /operations` et de `PUT /operations/1`

- Deux fois le même `POST /operations` : le serveur crée **deux opérations distinctes** (identifiants 1 et 2). Le POST n'est **pas idempotent**.
- Deux fois le même `PUT /operations/1` : l'opération 1 est remplacée deux fois par les mêmes données, le résultat final est le même. Le PUT est **idempotent**.

La réponse n'est pas différente de celle du TP2 : cette propriété vient de la sémantique des verbes HTTP, pas de la technologie utilisée.