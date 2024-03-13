package edu.upc.talent.swqa.campus.domain;

import edu.upc.talent.swqa.campus.infrastructure.PostgreSqlUsersRepository;
import edu.upc.talent.swqa.campus.infrastructure.SmtpEmailService;
import edu.upc.talent.swqa.jdbc.Database;

import java.util.Optional;

import static edu.upc.talent.swqa.jdbc.HikariCP.getDataSource;

public final class CampusApp {

  private final UsersRepository usersRepository;
  private final EmailService emailService;

  public CampusApp(final UsersRepository usersRepository, final EmailService emailService) {
    this.usersRepository = usersRepository;
    this.emailService = emailService;
  }

  public void sendMailToGroup(final String groupName, final String subject, final String body) {
    final var users = usersRepository.getUsersByGroup(groupName);
    users.forEach(u -> emailService.sendEmail(u, subject, body));
  }


  public void sendBirthdayEmails() {
    usersRepository.getUsersInBirthday().forEach(u ->
                                                       System.out.println("--------\n" +
                                                                          "to: " + u.email() + "\n" +
                                                                          "subject: Happy Campus Birthday!\n" +
                                                                          "body:\n" + "Happy campus birthday " +
                                                                          u.name() + " " + u.surname() + "!\n" +
                                                                          "You have been with us since " +
                                                                          u.createdAt() + "\n" +
                                                                          "--------\n")
    );
  }

  public void createUser(
        final String id,
        final String name,
        final String surname,
        final String email,
        final String role,
        final String groupName
  ) {
    usersRepository.createUser(id, name, surname, email, role, groupName);
  }

  public void createGroup(final String id, final String name) {
    usersRepository.createGroup(id, name);
  }

  public static CampusApp buildProductionApp() {
    final var dbHost = System.getenv("DATABASE_HOST");
    final var dbUser = System.getenv("DATABASE_USER");
    final var dbPassword = System.getenv("DATABASE_PASSWORD");
    final var db = new Database(getDataSource("jdbc:postgresql://" + dbHost + "/", dbUser, dbPassword));
    final var emailService = new SmtpEmailService();
    final var usersRepository = new PostgreSqlUsersRepository(db);
    return new CampusApp(usersRepository, emailService);
  }

  public void sendMailToGroupRole(final String groupName, final String roleName, final String subject, final String body) {
    final var users = usersRepository.getUsersByGroup(groupName);
    users.stream().filter(u -> u.role().equals(roleName))
          .forEach(u -> emailService.sendEmail(u, subject, body));
  }

    public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
      if (subject == null || subject.isBlank()) {
        throw new Exception("The email subject is mandatory");
      }

      final Optional<User> maybeUser = usersRepository.getUserById(id);

      if (maybeUser.isEmpty()) {
        throw new Exception("User " + id + " does not exist");
      }

      final User user = maybeUser.get();

      if (!user.role().equals("teacher")) {
        throw new Exception("User " + id + " is not a teacher");
      }

      emailService.sendEmail(user, subject, body);
    }

    public void sendEmailToTeacherIdWithConfirmation(String id, String subject, String body, boolean confirm) throws Exception {
      if (confirm) {
        if (body == null) {
          body = "";
        } else if (body.isBlank()) {
          throw new Exception("El cuerpo debería ser nulo. Cámbielo y mantenga marcada la casilla 'Confirmar'");
        }
      } else if (body == null) {
        throw new Exception("No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'");
      } else if (body.isBlank()) {
        throw new Exception("El cuerpo debería ser nulo. Cámbielo y marque la casilla 'Confirmar'");
      }

      sendEmailToTeacherId(id, subject, body);
    }
}
