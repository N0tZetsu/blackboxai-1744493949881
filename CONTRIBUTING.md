# Contributing to StandCore

Thank you for your interest in contributing to StandCore! We welcome contributions from the community to help make this plugin better.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/StandCore.git`
3. Create a new branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test your changes thoroughly
6. Commit your changes: `git commit -m "Add your commit message"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Create a Pull Request

## Development Environment

1. Install Java JDK 8 or higher
2. Install Maven
3. Use an IDE that supports Maven projects (IntelliJ IDEA recommended)
4. Import the project as a Maven project
5. Run `mvn clean install` to build the project

## Code Style Guidelines

1. Follow Java naming conventions
   - Classes: PascalCase
   - Methods and variables: camelCase
   - Constants: UPPER_SNAKE_CASE

2. Documentation
   - Add JavaDoc comments for all public methods
   - Include parameter descriptions and return values
   - Document any exceptions that may be thrown

3. Code Organization
   - Keep methods focused and concise
   - Use appropriate design patterns
   - Follow SOLID principles

4. Error Handling
   - Use appropriate exception handling
   - Log errors with sufficient context
   - Provide meaningful error messages

## Adding New Features

1. Commands
   - Create a new class in the `commands` package
   - Implement the `CommandExecutor` interface
   - Add command registration in `StandCore.java`
   - Add command details in `plugin.yml`
   - Update README.md with new command documentation

2. Listeners
   - Create a new class in the `listeners` package
   - Implement the `Listener` interface
   - Register the listener in `StandCore.java`
   - Document any new events being handled

3. Configuration
   - Add new configuration options in appropriate .yml files
   - Add default values
   - Document new configuration options in README.md
   - Implement proper config loading and validation

## Testing

1. Manual Testing
   - Test all new features thoroughly
   - Test interaction with existing features
   - Test with different permission configurations
   - Test edge cases and error handling

2. Server Testing
   - Test on a clean server installation
   - Test with other plugins
   - Test performance impact
   - Test memory usage

## Pull Request Guidelines

1. PR Description
   - Clearly describe the changes
   - Reference any related issues
   - List any breaking changes
   - Include testing procedures

2. Code Review
   - Respond to review comments promptly
   - Make requested changes
   - Keep discussions constructive
   - Be open to suggestions

3. Documentation
   - Update README.md if needed
   - Update configuration documentation
   - Add/update JavaDoc comments
   - Include code examples if relevant

## Community Guidelines

1. Be respectful and professional
2. Help others when possible
3. Follow the code of conduct
4. Keep discussions focused and constructive

## Need Help?

- Check existing documentation
- Search through issues
- Create a new issue for questions
- Join our Discord server for support

## License

By contributing to StandCore, you agree that your contributions will be licensed under the same license as the project.

Thank you for contributing to StandCore!
