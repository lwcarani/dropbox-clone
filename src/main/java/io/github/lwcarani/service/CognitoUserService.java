package io.github.lwcarani.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;
import com.amazonaws.services.cognitoidp.model.DeleteUserRequest;
import com.amazonaws.services.cognitoidp.model.GetUserRequest;
import com.amazonaws.services.cognitoidp.model.GetUserResult;
import com.amazonaws.services.cognitoidp.model.GlobalSignOutRequest;
import com.amazonaws.services.cognitoidp.model.MessageActionType;

import io.github.lwcarani.model.User;
import jakarta.annotation.PostConstruct;

@Service
public class CognitoUserService implements UserService {

	private AWSCognitoIdentityProvider cognitoClient;

	@Value("${aws.cognito.userPoolId}")
	private String userPoolId;

	@Value("${aws.cognito.clientId}")
	private String clientId;

	@Value("${aws.cognito.clientSecret}")
	private String clientSecret;

	@Value("${aws.accessKey}")
	private String accessKey;

	@Value("${aws.secretKey}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@PostConstruct
	public void init() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		this.cognitoClient = AWSCognitoIdentityProviderClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(region).build();
	}

	private String calculateSecretHash(String username) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec signingKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			mac.init(signingKey);
			mac.update(username.getBytes(StandardCharsets.UTF_8));
			byte[] rawHmac = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(rawHmac);
		} catch (Exception e) {
			throw new RuntimeException("Error calculating SECRET_HASH", e);
		}
	}

	@Override
	public AuthenticationResultType authenticateUser(String username, String password) {
		try {
			final Map<String, String> authParams = new HashMap<>();
			authParams.put("USERNAME", username);
			authParams.put("PASSWORD", password);
			authParams.put("SECRET_HASH", calculateSecretHash(username));

			final AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
					.withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH).withUserPoolId(userPoolId).withClientId(clientId)
					.withAuthParameters(authParams);

			AdminInitiateAuthResult result = cognitoClient.adminInitiateAuth(authRequest);
			return result.getAuthenticationResult();
		} catch (Exception e) {
			System.err.println("Error during authentication: " + e.getMessage());
			return null;
		}
	}

	@Override
	public boolean authenticateUserSession(String accessToken) {
		try {
			GetUserRequest getUserRequest = new GetUserRequest().withAccessToken(accessToken);
			cognitoClient.getUser(getUserRequest);
			return true;
		} catch (Exception e) {
			System.err.println("Error validating token: " + e.getMessage());
			return false;
		}
	}

	@Override
	public User createUser(String username, String password, String email) {
		try {
			AttributeType emailAttr = new AttributeType().withName("email").withValue(email);

			AdminCreateUserRequest createUserRequest = new AdminCreateUserRequest().withUserPoolId(userPoolId)
					.withUsername(username).withTemporaryPassword(password).withUserAttributes(emailAttr)
					.withMessageAction(MessageActionType.SUPPRESS);

			AdminCreateUserResult createUserResult = cognitoClient.adminCreateUser(createUserRequest);

			AdminSetUserPasswordRequest setPasswordRequest = new AdminSetUserPasswordRequest()
					.withUserPoolId(userPoolId).withUsername(username).withPassword(password).withPermanent(true);

			cognitoClient.adminSetUserPassword(setPasswordRequest);

			String userId = createUserResult.getUser().getAttributes().stream()
					.filter(attr -> "sub".equals(attr.getName())).findFirst().map(AttributeType::getValue).orElse(null);

			return new User(username, email, userId);
		} catch (Exception e) {
			System.err.println("Error creating user: " + e.getMessage());
			throw new RuntimeException("Failed to create user", e);
		}
	}

	public String getUserId(String accessToken) {
		try {
			GetUserRequest getUserRequest = new GetUserRequest().withAccessToken(accessToken);
			GetUserResult getUserResult = cognitoClient.getUser(getUserRequest);
			return getUserResult.getUserAttributes().stream().filter(attr -> "sub".equals(attr.getName())).findFirst()
					.map(AttributeType::getValue).orElse(null);
		} catch (Exception e) {
			System.err.println("Error getting user ID: " + e.getMessage());
			return null;
		}
	}

	public String getEmail(String accessToken) {
		try {
			GetUserRequest getUserRequest = new GetUserRequest().withAccessToken(accessToken);
			GetUserResult getUserResult = cognitoClient.getUser(getUserRequest);
			return getUserResult.getUserAttributes().stream().filter(attr -> "email".equals(attr.getName())).findFirst()
					.map(AttributeType::getValue).orElse(null);
		} catch (Exception e) {
			System.err.println("Error getting user email: " + e.getMessage());
			return null;
		}
	}

	@Override
	public void logout(String accessToken) {
		try {
			GlobalSignOutRequest signOutRequest = new GlobalSignOutRequest().withAccessToken(accessToken);
			cognitoClient.globalSignOut(signOutRequest);
		} catch (Exception e) {
			System.err.println("Error during logout: " + e.getMessage());
		}
	}

	@Override
	public void deleteUser(String accessToken) {
		try {
			DeleteUserRequest deleteUserRequest = new DeleteUserRequest().withAccessToken(accessToken);
			cognitoClient.deleteUser(deleteUserRequest);
		} catch (Exception e) {
			System.err.println("Error deleting user: " + e.getMessage());
		}
	}
}