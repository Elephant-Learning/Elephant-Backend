package me.elephantsuite.deck.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import me.elephantsuite.deck.Deck;
import me.elephantsuite.deck.DeckRepositoryService;
import me.elephantsuite.deck.DeckVisibility;
import me.elephantsuite.deck.card.Card;
import me.elephantsuite.deck.card.CardService;
import me.elephantsuite.response.Response;
import me.elephantsuite.response.ResponseBuilder;
import me.elephantsuite.response.ResponseStatus;
import me.elephantsuite.user.ElephantUser;
import me.elephantsuite.user.ElephantUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional
public class DeckService {

	private final ElephantUserService userService;

	private final DeckRepositoryService service;

	private final CardService cardService;

	public Response createDeck(DeckRequest.CreateDeck request) {
		Map<String, List<String>> terms = request.getTerms();
		long authorId = request.getAuthorId();
		String name = request.getName();
		DeckVisibility visibility = request.getVisibility();

		ElephantUser user = userService.getUserById(authorId);

		if (user == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Author ID Given!")
				.addObject("request", request)
				.build();
		}

		if (!user.isEnabled()) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "User not enabled!")
				.addObject("user", user)
				.build();
		}

		Deck deck = new Deck(null, user, name, visibility);

		List<Card> cards = convertToCards(terms, deck ,cardService);

		deck.setCards(cards);

		user.getDecks().add(deck);

		deck = service.saveDeck(deck);

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Created Deck!")
			.addObject("user", user)
			.addObject("deck", deck)
			.build();
	}

	public static List<Card> convertToCards(Map<String, List<String>> cardsMap, Deck deck, CardService cardService) {
		List<Card> cards = new ArrayList<>();

		cardsMap.forEach((s, strings) -> {
			Card card = new Card(s, strings, deck);
			cards.add(card);
			cardService.saveCard(card);
		});

		return cards;
	}

	public Response likeDeck(DeckRequest.LikeDeck likeDeck) {
		Deck deck = service.getDeckById(likeDeck.getDeckId());

		ElephantUser user = userService.getUserById(likeDeck.getUserId());

		if (deck == null || user == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck Or User ID!")
				.addObject("deckId", likeDeck.getDeckId())
				.addObject("userId", likeDeck.getUserId())
				.build();
		}

		if (!user.isEnabled()) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "User not enabled!")
				.addObject("user", user)
				.build();
		}

		deck.likeDeck();

		deck = service.saveDeck(deck);

		user.getLikedDecksIds().add(likeDeck.getDeckId());

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Favorited Deck!")
			.addObject("deck", deck)
			.addObject("user", user)
			.build();
	}

	public Response getAllDecks() {
		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Retrieved All Decks!")
			.addObject("decks", this.service.getAllDecks())
			.build();


	}

	public Response renameDeck(DeckRequest.RenameDeck renameDeck) {
		Deck deck = service.getDeckById(renameDeck.getDeckId());

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("deckId", renameDeck.getDeckId())
				.build();
		}

		deck.setName(renameDeck.getNewName());

		deck = service.saveDeck(deck);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Renamed Deck!")
			.addObject("deck", deck)
			.build();
	}


	public Response resetTerms(DeckRequest.ResetTerms resetTerms) {
		Deck deck = service.getDeckById(resetTerms.getDeckId());

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("deckId", resetTerms.getDeckId())
				.build();
		}

		deck.resetTerms(resetTerms.getNewTerms(), this.cardService);

		deck = service.saveDeck(deck);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Added Terms to Deck!")
			.addObject("deck", deck)
			.build();
	}

	public Response deleteDeck(long id) {
		Deck deck = service.getDeckById(id);

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("deckId", id)
				.build();
		}

		service.deleteDeck(deck, cardService);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Deleted Deck!")
			.build();
	}

	public Response changeVisibility(DeckRequest.ChangeVisiblity changeVisiblity) {
		long id = changeVisiblity.getDeckId();
		DeckVisibility visibility = changeVisiblity.getVisibility();

		Deck deck = service.getDeckById(id);

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("deckId", id)
				.build();
		}

		deck.setVisibility(visibility);

		deck = service.saveDeck(deck);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Changed Visibility of Deck!")
			.addObject("deck", deck)
			.build();
	}

	public Response shareDeck(DeckRequest.ShareDeck shareDeck) {
		long userId = shareDeck.getSharedUserId();
		long deckId = shareDeck.getDeckId();

		Deck deck = service.getDeckById(deckId);
		ElephantUser user = userService.getUserById(userId);

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("deckId", deckId)
				.build();
		}

		if (user == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.SUCCESS, "Invalid User ID!")
				.addObject("userId", userId)
				.build();
		}

		if (user.equals(deck.getAuthor())) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Cannot Share Own Deck With Yourself!")
				.addObject("user", user)
				.addObject("deck", deck)
				.build();
		}

		if (deck.getVisibility().equals(DeckVisibility.PRIVATE)) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Cannot share a deck that is private!")
				.addObject("deck", deck)
				.addObject("sharedUser", user)
				.build();
		}

		deck.getSharedUsersIds().add(userId);
		user.getSharedDeckIds().add(deckId);

		deck = service.saveDeck(deck);
		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Shared Deck with User!")
			.addObject("deck", deck)
			.addObject("sharedUser", user)
			.build();
	}

	public Response getById(long id) {
		Deck deck = service.getDeckById(id);

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("id", id)
				.build();
		}

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Retrieved Deck By ID!")
			.addObject("deck", deck)
			.build();
	}

	public Response unlikeDeck(DeckRequest.LikeDeck likeDeck) {
		Deck deck = service.getDeckById(likeDeck.getDeckId());

		ElephantUser user = userService.getUserById(likeDeck.getUserId());

		if (deck == null || user == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck Or User ID!")
				.addObject("deckId", likeDeck.getDeckId())
				.addObject("userId", likeDeck.getUserId())
				.build();
		}

		if (!user.isEnabled()) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "User not enabled!")
				.addObject("user", user)
				.build();
		}

		deck.unlikeDeck();

		deck = service.saveDeck(deck);

		user.getLikedDecksIds().remove(likeDeck.getDeckId());

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Unliked Deck!")
			.addObject("deck", deck)
			.addObject("user", user)
			.build();
	}

	public Response getByName(String name) {
		List<Deck> decks = service.getAllDecks();

		List<Deck> filteredDecks = decks
			.stream()
			.filter(deck -> deck.getName().contains(name))
			.collect(Collectors.toList());

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Retrieved Decks with Name!")
			.addObject("decks", filteredDecks)
			.build();
	}

    public Response setName(DeckRequest.SetName setName) {
		long deckId = setName.getDeckId();
		String name = setName.getName();

		Deck deck = service.getDeckById(deckId);

		if (deck == null) {
			return ResponseBuilder
				.create()
				.addResponse(ResponseStatus.FAILURE, "Invalid Deck ID!")
				.addObject("id", deckId)
				.build();
		}

		deck.setName(name);

		service.saveDeck(deck);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Set Deck Name!")
			.addObject("deck", deck)
			.build();

	}
}
