/**
 * This JAVA class implements the following endpoints:
 * a. /question/create
 * b. /question/all
 * c. /question/edit/{questionId}
 * d. /question/delete/{questionId}
 * e. /question/all/{userId}
 * @author  Anup Shanbhag (shanbhaganup@gmail.com)
 * @version 1.0
 * @since   2020-04-16
 */

package com.upgrad.quora.api.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.upgrad.quora.api.model.QuestionDeleteResponse;
import com.upgrad.quora.api.model.QuestionDetailsResponse;
import com.upgrad.quora.api.model.QuestionEditRequest;
import com.upgrad.quora.api.model.QuestionEditResponse;
import com.upgrad.quora.api.model.QuestionRequest;
import com.upgrad.quora.api.model.QuestionResponse;
import com.upgrad.quora.service.business.QuestionService;
import com.upgrad.quora.service.business.UserBusinessService;
import com.upgrad.quora.service.constants.QuestionStatus;
import com.upgrad.quora.service.entity.QuestionEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.InvalidQuestionException;
import com.upgrad.quora.service.exception.UserNotFoundException;
import com.upgrad.quora.service.constants.GetCurrentUserAction;

@RestController
@RequestMapping("/question")
public class QuestionController {

	@Autowired
	QuestionService questionService;

	@Autowired
	UserBusinessService userService;

	/**
	 * This is used to create a question in the application which will be shown to
	 * all users. It takes input for content of the question & authorization token
	 * and creates the question in the database.
	 * 
	 * @param authorization Authorization token from request header
	 * @param request       An input request with question content
	 * @return Response Entity with questionId, message and Http Status Code
	 * @throws AuthorizationFailedException if the authorization token is invalid,
	 *                                      expired or not found
	 */
	@RequestMapping(path = "/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<QuestionResponse> createQuestion(@RequestHeader("authorization") String authorization,
			QuestionRequest request) throws AuthorizationFailedException {
		String token = (authorization.contains("Bearer ")) ? StringUtils.substringAfter(authorization, "Bearer ")
				: authorization;
		UserEntity user = userService.getCurrentUser(token, GetCurrentUserAction.CREATE_QUESTION);
		QuestionEntity question = new QuestionEntity();
		question.setContent(request.getContent());
		question.setDate(LocalDate.now());
		question.setUuid(UUID.randomUUID().toString());
		question.setUser(user);
		question = questionService.createQuestion(question);
		QuestionResponse response = new QuestionResponse();
		response.setId(question.getUuid());
		response.setStatus(QuestionStatus.QUESTION_CREATED.getStatus());
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	/**
	 * This is used to fetch all questions have been posted in the application. It
	 * takes authorization token to list all available questions from the
	 * application.
	 * 
	 * @param authorization Authorization token from request header
	 * @return List of all questions posted in the application
	 * @throws AuthorizationFailedException if the authorization token is invalid,
	 *                                      expired or not found.
	 */
	@RequestMapping(path = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<QuestionDetailsResponse>> getAllQuestions(
			@RequestHeader("authorization") String authorization) throws AuthorizationFailedException {
		String token = (authorization.contains("Bearer ")) ? StringUtils.substringAfter(authorization, "Bearer ")
				: authorization;
		userService.getCurrentUser(token, GetCurrentUserAction.GET_ALL_QUESTIONS);
		List<QuestionDetailsResponse> responseItems = this.mapListResponseItems(questionService.getAllQuestions());
		if (responseItems.isEmpty()) {
			return new ResponseEntity<>(responseItems, HttpStatus.NO_CONTENT);
		} else {
			return new ResponseEntity<>(responseItems, HttpStatus.OK);
		}
	}

	/**
	 * This is used to edit a question that has been posted by a user. Note, only
	 * the question owner or an admin can edit a question. It takes questionId,
	 * question content and authorization token to find and update a question in the
	 * database.
	 * 
	 * @param authorization Authorization token from request header
	 * @param questionId    Id of the question to delete
	 * @param request       An input request with question content
	 * @return Response Entity with questionId, message and Http Status Code
	 * @throws AuthorizationFailedException if the authorization token is invalid,
	 *                                      expired or not found.
	 * @throws AuthorizationFailedException if a non-owner attempts to edit a
	 *                                      question.
	 * @throws InvalidQuestionException     if a question with input questionId
	 *                                      doesn't exist
	 */
	@RequestMapping(path = "/edit/{questionId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<QuestionEditResponse> editQuestion(@RequestHeader("authorization") String authorization,
			@PathVariable("questionId") String questionId, QuestionEditRequest request)
			throws AuthorizationFailedException, InvalidQuestionException {
		String token = (authorization.contains("Bearer ")) ? StringUtils.substringAfter(authorization, "Bearer ")
				: authorization;
		UserEntity user = userService.getCurrentUser(token, GetCurrentUserAction.EDIT_QUESTION);
		QuestionEntity question = questionService.getQuestion(questionId);
		question.setContent(request.getContent());
		questionId = questionService.editQuestion(question, user);
		QuestionEditResponse response = new QuestionEditResponse();
		response.setId(questionId);
		response.setStatus(QuestionStatus.QUESTION_EDITED.getStatus());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/**
	 * This is used to delete a question that has been posted by a user. Note, only
	 * the question owner or an admin can delete a question. It takes questionId and
	 * authorization token to find and delete a question in the database.
	 * 
	 * @param authorization Authorization token from request header
	 * @param questionId    Id of the question to delete
	 * @return Response Entity with questionId, message and Http Status Code
	 * @throws AuthorizationFailedException if the authorization token is invalid,
	 *                                      expired or not found.
	 * @throws AuthorizationFailedException if a non-admin non-owner(question)
	 *                                      attempts to delete a question.
	 * @throws InvalidQuestionException     if a question with input questionId
	 *                                      doesn't exist
	 */
	@RequestMapping(path = "/delete/{questionId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<QuestionDeleteResponse> deleteQuestion(@RequestHeader("authorization") String authorization,
			@PathVariable("questionId") String questionId)
			throws AuthorizationFailedException, InvalidQuestionException {
		String token = (authorization.contains("Bearer ")) ? StringUtils.substringAfter(authorization, "Bearer ")
				: authorization;
		UserEntity user = userService.getCurrentUser(token, GetCurrentUserAction.DELETE_QUESTION);
		QuestionEntity question = questionService.getQuestion(questionId);
		questionId = questionService.deleteQuestion(question, user);
		QuestionDeleteResponse response = new QuestionDeleteResponse();
		response.setId(questionId);
		response.setStatus(QuestionStatus.QUESTION_DELETED.getStatus());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/**
	 * This is used to fetch all questions posted by a specific user in the
	 * application. It takes authorization token and the user id of the user and
	 * fetches list questions posted by the user.
	 * 
	 * @param authorization Authorization token from request header
	 * @param userId        Id of the user whose question need to be retrieved
	 * @return Response Entity with Http Status Code and question details (id &
	 *         content) for all questions posted by user with input user id
	 * @throws AuthorizationFailedException if the authorization token is invalid,
	 *                                      expired or not found
	 * @throws AuthorizationFailedException if userId is invalid (no such user
	 *                                      exists)
	 */
	@RequestMapping(path = "/all/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<QuestionDetailsResponse>> getUserQuestions(
			@RequestHeader("authorization") String authorization, @PathVariable("userId") String userId)
			throws AuthorizationFailedException, UserNotFoundException {
		String token = (authorization.contains("Bearer ")) ? StringUtils.substringAfter(authorization, "Bearer ")
				: authorization;
		UserEntity user = userService.getUser(userId, token, GetCurrentUserAction.GET_ALL_QUESTIONS_BY_USER);
		List<QuestionDetailsResponse> responseItems = this.mapListResponseItems(questionService.getUserQuestions(user));
		if (responseItems.isEmpty()) {
			return new ResponseEntity<>(responseItems, HttpStatus.NO_CONTENT);
		} else {
			return new ResponseEntity<>(responseItems, HttpStatus.OK);
		}
	}

	/**
	 * This method takes a list of question items and maps it to the response items
	 * 
	 * @param questions List of questions
	 * @return List of response items with question details
	 */
	private List<QuestionDetailsResponse> mapListResponseItems(List<QuestionEntity> questions) {
		List<QuestionDetailsResponse> response = new ArrayList<>();
		questions.forEach(question -> response
				.add(new QuestionDetailsResponse().id(question.getUuid()).content(question.getContent())));
		return response;
	}

}
