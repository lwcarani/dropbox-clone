# Dropbox Clone

This project is a command-line interface (CLI) application (written in Java) that mimics some of the core functionalities of Dropbox, allowing users to synchronize files between their local machine and cloud storage (in this particular implementation, Amazon Web Services (AWS) Simple Storage Service (S3)).

## Features

- User authentication
    - new account: `signup`
    - login: `login`
    - logout: `logout`
    - delete account: `delete_account`
- File and folder operations 
    - change current working directory: `cd` 
    - create: `mkdir`
    - delete: `rm`
    - list: `ls`
- Sync files between local machine and cloud storage (`push`, `pull`)
- Change root directory (`change_root`)
- User preferences storage

## Technology Stack

- Java 17
- Spring Boot 3.1.0
- AWS SDK for Java 1.12.465
- AWS S3 for file storage
- AWS Cognito for user authentication

## Project Structure

The project is organized into several packages:

- `io.github.lwcarani`: Main application package
- `io.github.lwcarani.cli`: Contains the CLI runner
- `io.github.lwcarani.config`: AWS configuration
- `io.github.lwcarani.model`: Data models
- `io.github.lwcarani.service`: Service interfaces and implementations
- `io.github.lwcarani.util`: Utility classes

## Setup

1. Ensure you have Java 17 installed.
2. Clone the repository:
   ```
   git clone https://github.com/your-username/dropbox-clone.git
   ```
3. Navigate to the project directory:
   ```
   cd dropbox-clone
   ```
4. Create an `application.properties` file in the `src/main/resources` directory with the following content:
   ```
   aws.s3.bucket-user-preferences=<your_s3_bucket_name_for_user_preferences>
   aws.s3.bucket-user-storage=<your_s3_bucket_name_for_user_storage>
   aws.accessKey=<your_aws_access_key>
   aws.secretKey=<your_aws_secret_key>
   aws.region=<your_aws_region>
   aws.cognito.userPoolId=<your_cognito_user_pool_id>
   aws.cognito.clientId=<your_cognito_client_id>
   aws.cognito.clientSecret=<your_cognito_client_secret>
   spring.main.banner-mode=console
   logging.level.root=ERROR
   ```

   Replace the placeholder values with your actual AWS credentials and resource identifiers.

## Dependencies

The project uses the following main dependencies:

- Spring Boot Starter Web
- Spring Boot Starter Security
- AWS Java SDK for Amazon Cognito Identity Provider
- AWS Java SDK for Amazon S3

For a full list of dependencies, please refer to the `pom.xml` file.

## Building

The project uses the Spring Boot Maven plugin for building. The main class is set to `io.github.lwcarani.DropboxCloneApplication`.

To build a JAR file in Eclipse IDE using Maven, follow these steps:

1. Ensure your project is set up as a Maven project in Eclipse.
2. Open the project in Eclipse.
3. Right-click on the project in the Package Explorer.
4. Navigate to "Run As" > "Maven build..."
5. In the "Goals" field, enter "clean package"
6. Click "Run"

Maven will then compile your code, run any tests, and package your application into a JAR file. The JAR file will typically be created in the `target` directory of your project.

## Running the Application

Once you've built a JAR file, you can run the application from the command line, like so:

```
java -jar target\dropbox-clone-0.0.2.jar
```

## Usage

Once the application is running, you can use the following commands:

- `signup`: Create a new user account
- `login`: Log in to your account
- `logout`: Log out of your account
- `delete_account`: Permanently delete your account
- `push`: Upload all local files and folders to cloud storage
- `pull`: Download all files and folders from cloud to local machine
- `mkdir <folder_name>`: Create a new directory
- `cd <path>`: Change current directory
- `ls <path>`: List contents of current or specified directory
- `rm <path>`: Delete a directory and its contents (both locally and in cloud)
- `change_root <path>`: Set a new root directory for your Dropbox Clone files
- `help`: Display available commands
- `exit`: Exit the application

## Contributing

Feedback, bug reports, issues, and pull requests welcome!

## Acknowledgements
Thanks to [John Crickett](https://github.com/JohnCrickett) for the idea from his site, [Coding Challenges](https://codingchallenges.fyi/challenges/challenge-dropbox/)!
