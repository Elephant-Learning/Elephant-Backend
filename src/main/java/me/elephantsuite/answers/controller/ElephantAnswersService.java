package me.elephantsuite.answers.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import me.elephantsuite.answers.ElephantAnswer;
import me.elephantsuite.answers.ElephantAnswerRepository;
import me.elephantsuite.answers.ElephantAnswerRepositoryService;
import me.elephantsuite.answers.comment.Comment;
import me.elephantsuite.answers.comment.CommentRepositoryService;
import me.elephantsuite.answers.reply.Reply;
import me.elephantsuite.answers.reply.ReplyRepositoryService;
import me.elephantsuite.registration.RegistrationService;
import me.elephantsuite.response.api.Response;
import me.elephantsuite.response.api.ResponseBuilder;
import me.elephantsuite.response.exception.InvalidIdException;
import me.elephantsuite.response.exception.InvalidIdType;
import me.elephantsuite.response.exception.InvalidTagInputException;
import me.elephantsuite.response.util.ResponseStatus;
import me.elephantsuite.response.util.ResponseUtil;
import me.elephantsuite.user.ElephantUser;
import me.elephantsuite.user.ElephantUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ElephantAnswersService {

	private final ElephantAnswerRepositoryService service;

	private final ElephantUserService userService;

	private final CommentRepositoryService commentService;

	private final ReplyRepositoryService replyService;

	public Response createAnswer(ElephantAnswersRequest.CreateAnswer request) {
		String description = request.getDescription();
		String title = request.getTitle();
		long userId = request.getUserId();

		ElephantUser user = ResponseUtil.checkUserValid(userId, userService);

		if (RegistrationService.isInvalidName(title)) {
			throw new InvalidTagInputException(description, title);
		}

		ElephantAnswer answer = new ElephantAnswer(title, description, user);

		user.getAnswers().add(answer);

		answer = service.save(answer);

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Created Answer!")
			.addObject("answer", answer)
			.build();
	}


	public Response changeTitle(ElephantAnswersRequest.ChangeTitle request, boolean description) {
		String title = request.getValue();
		long answerId = request.getAnswerId();

		ElephantAnswer answer = service.getAnswerById(answerId);

		if (answer == null) {
			throw new InvalidIdException(request, InvalidIdType.ANSWER);
		}

		if (RegistrationService.isInvalidName(title)) {
			throw new InvalidTagInputException(title);
		}
		if (description) {
			answer.setDescription(title);
		} else {
			answer.setTitle(title);
		}

		answer.updateLastUpdatedTime();

		answer = service.save(answer);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Changed title of Answer!")
			.addObject("answer", answer)
			.build();
	}

	public Response setAnswered(long answerId) {

		ElephantAnswer answer = service.getAnswerById(answerId);

		if (answer == null) {
			throw new InvalidIdException(answerId, InvalidIdType.ANSWER);
		}

		answer.setAnswered(true);

		answer.updateLastUpdatedTime();

		answer = service.save(answer);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Set Answer to be Answered!")
			.addObject("answer", answer)
			.build();
	}


	public Response setTags(ElephantAnswersRequest.SetTags request) {
		List<Integer> tags = request.getTags();
		long answerId = request.getAnswerId();

		ElephantAnswer answer = service.getAnswerById(answerId);

		if (answer == null) {
			throw new InvalidIdException(request, InvalidIdType.ANSWER);
		}

		answer.setTags(tags);

		answer.updateLastUpdatedTime();

		answer = service.save(answer);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Set Answer's tags!")
			.addObject("answer", answer)
			.build();
	}

	public Response likeAnswer(ElephantAnswersRequest.LikeAnswer request, boolean like) {
		long answerId = request.getAnswerId();
		long userId = request.getUserId();

		ElephantAnswer answer = service.getAnswerById(answerId);
		ElephantUser user = userService.getUserById(userId);

		if (answer == null || user == null) {
			throw new InvalidIdException(request, InvalidIdType.ANSWER, InvalidIdType.USER);
		}

		if (like) {
			answer.incrementLikes();
		} else {
			answer.decrementLikes();
		}

		answer.updateLastUpdatedTime();

		answer = service.save(answer);

		if (like) {
			user.getElephantAnswersLiked().add(answerId);
		} else {
			user.getElephantAnswersLiked().remove(answerId);
		}

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Changed Like Count!")
			.addObject("answer", answer)
			.build();
	}

	public Response deleteAnswer(long answerId) {
		ElephantAnswer answer = service.getAnswerById(answerId);

		if (answer == null) {
			throw new InvalidIdException(answerId, InvalidIdType.ANSWER);
		}

		answer.getUser().getAnswers().remove(answer);

		answer.updateLastUpdatedTime();

		userService.saveUser(answer.getUser());

		answer.setUser(null);

		service.delete(answer);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Deleted Answer!")
			.build();
	}

	public Response searchByName(String name) {
		List<ElephantAnswer> answers = service
			.getAllAnswers()
			.stream()
			.filter(answer -> StringUtils.containsIgnoreCase(answer.getTitle(), name))
			.toList();

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Retrieved answers with name!")
			.addObject("answers", answers)
			.build();
	}

	public Response createComment(ElephantAnswersRequest.CreateComment request) {
		long answerId = request.getAnswerId();
		long userId = request.getUserId();
		String description = request.getDescription();

		ElephantAnswer answer = service.getAnswerById(answerId);
		ElephantUser user = userService.getUserById(userId);

		if (answer == null || user == null) {
			throw new InvalidIdException(request, InvalidIdType.ANSWER, InvalidIdType.USER);
		}

		Comment comment = new Comment(description, answer, user);
		answer.getComments().add(comment);
		answer.updateLastUpdatedTime();

		comment = commentService.save(comment);

		answer = service.save(answer);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Created Comment!")
			.addObject("comment", comment)
			.build();
	}

	public Response editComment(ElephantAnswersRequest.EditComment request) {
		long commentId = request.getCommentId();
		String description = request.getDescription();

		Comment comment = commentService.getCommentById(commentId);

		if (comment == null) {
			throw new InvalidIdException(request, InvalidIdType.COMMENT);
		}

		comment.setDescription(description);

		comment = commentService.save(comment);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Edited Comment!")
			.addObject("comment", comment)
			.build();
	}

	public Response likeComment(long commentId, boolean like) {
		Comment comment = commentService.getCommentById(commentId);

		if (comment == null) {
			throw new InvalidIdException(comment, InvalidIdType.COMMENT);
		}

		if (like) {
			comment.incrementLikes();
		} else {
			comment.decrementLikes();
		}

		comment = commentService.save(comment);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, (like ? "Liked" : "Disliked") + " Comment!")
			.addObject("comment", comment)
			.build();
	}

	public Response deleteComment(long commentId) {
		Comment comment = commentService.getCommentById(commentId);

		if (comment == null) {
			throw new InvalidIdException(comment, InvalidIdType.COMMENT);
		}


		comment.getAnswer().getComments().remove(comment);

		service.save(comment.getAnswer());

		comment.setAnswer(null);

		commentService.delete(comment);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Deleted Comment!")
			.build();
	}

	public Response createReply(ElephantAnswersRequest.CreateReply request) {
		String text = request.getText();
		long commentId = request.getCommentId();
		long userId = request.getUserId();

		Comment comment = commentService.getCommentById(commentId);
		ElephantUser user = userService.getUserById(userId);

		if (comment == null || user == null) {
			throw new InvalidIdException(request, InvalidIdType.COMMENT, InvalidIdType.USER);
		}

		Reply reply = new Reply(text, comment, user);

		comment.getReplies().add(reply);

		reply = replyService.save(reply);
		comment = commentService.save(comment);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS,"Created Reply!")
			.build();
	}


	public Response editReply(ElephantAnswersRequest.EditReply request) {
		long replyId = request.getReplyId();
		String text = request.getText();
		
		Reply reply = replyService.getReplyById(replyId);
		
		if (reply == null) {
			throw new InvalidIdException(request, InvalidIdType.REPLY);
		}
		
		reply.setText(text);
		
		reply = replyService.save(reply);
		
		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Edited Reply!")
			.addObject("reply", reply)
			.build();
	}

	public Response deleteReply(long replyId) {
		Reply reply = replyService.getReplyById(replyId);

		if (reply == null) {
			throw new InvalidIdException(replyId, InvalidIdType.REPLY);
		}


		reply.getComment().getReplies().remove(reply);

		commentService.save(reply.getComment());

		reply.setComment(null);

		replyService.delete(reply);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Deleted Comment!")
			.build();
	}

	public Response increaseScore(ElephantAnswersRequest.IncreaseScore request) {
		int score = request.getScore();
		long userId = request.getUserId();

		ElephantUser user = userService.getUserById(userId);

		if (user == null) {
			throw new InvalidIdException(request, InvalidIdType.USER);
		}

		user.setElephantAnswersScore(user.getElephantAnswersScore() + score);

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Increased User Score!")
			.addObject("user", user)
			.build();
	}

	public Response setUserTags(ElephantAnswersRequest.SetUserTags request) {
		List<Integer> tags = request.getTags();
		long userId = request.getUserId();

		ElephantUser user = userService.getUserById(userId);

		if (user == null) {
			throw new InvalidIdException(request, InvalidIdType.USER);
		}

		user.setElephantAnswersTags(tags);

		user = userService.saveUser(user);

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Set User Tags!")
			.addObject("user", user)
			.build();
	}

	public Response getAnswersForUser(ElephantAnswersRequest.AnswersForUser request) {
		long userId = request.getUserId();
		ElephantUser user = userService.getUserById(userId);

		if (user == null) {
			throw new InvalidIdException(request, InvalidIdType.USER);
		}

		List<ElephantAnswer> sorted = sortAnswersOnDate(service.getAllAnswers().subList(0, service.getAllAnswers().size() >= 100 ? 101 : service.getAllAnswers().size()));

		return ResponseBuilder
			.create()
			.addResponse(ResponseStatus.SUCCESS, "Retrieved Answers For User!")
			.addObject("answers", sorted)
			.build();
	}

	private static List<ElephantAnswer> sortAnswersOnDate(List<ElephantAnswer> answers) {
		answers.sort((o1, o2) -> {
			if (o1.getLastUpdated().isBefore(o2.getLastUpdated())) {
				return -1;
			} else if (o1.getLastUpdated().isAfter(o2.getLastUpdated())) {
				return 1;
			}

			return 0;
		});

		return answers;
	}
}
