package edu.upc.talent.swqa.campus.domain.test;

import edu.upc.talent.swqa.campus.domain.CampusApp;
import edu.upc.talent.swqa.campus.test.utils.CampusAppState;
import edu.upc.talent.swqa.campus.test.utils.Group;
import edu.upc.talent.swqa.campus.test.utils.InMemoryUsersRepository;
import edu.upc.talent.swqa.campus.test.utils.SentEmail;
import static edu.upc.talent.swqa.campus.test.utils.TestFixtures.defaultInitialState;
import edu.upc.talent.swqa.campus.test.utils.UsersRepositoryState;
import static edu.upc.talent.swqa.test.utils.Asserts.assertEquals;
import static edu.upc.talent.swqa.util.Utils.union;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

public final class CampusAppTest {

  private CampusAppState state;

  private CampusApp getApp(final CampusAppState initialState) {
    state = initialState.copy();
    return new CampusApp(
          new InMemoryUsersRepository(state.usersRepositoryState()),
          new InMemoryEmailService(state.sentEmails())
    );
  }

  private CampusAppState getFinalState() {
    return state;
  }

  @Test
  public void testCreateGroup() {
    final var app = getApp(defaultInitialState);
    final var newGroup = new Group("0", "bigdata");
    app.createGroup(newGroup.id(),newGroup.name());
    final var expectedFinalState = new CampusAppState(
          new UsersRepositoryState(
                defaultInitialState.usersRepositoryState().users(),
                union(defaultInitialState.usersRepositoryState().groups(), Set.of(newGroup))
          ),
          Set.of()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToGroup() {
    final var app = getApp(defaultInitialState);
    final var subject = "New campus!";
    final var body = "Hello everyone! We just created a new virtual campus!";
    app.sendMailToGroup("swqa", subject, body);
    final var expectedFinalState = new CampusAppState(
          defaultInitialState.usersRepositoryState(),
          Set.of(
                new SentEmail("john.doe@example.com", subject, body),
                new SentEmail("jane.doe@example.com", subject, body),
                new SentEmail("mariah.hairam@example.com", subject, body)
          )
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToGroupRole() {
    final var app = getApp(defaultInitialState);
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";
    app.sendMailToGroupRole("swqa", "teacher", subject, body);
    final var expectedFinalState = new CampusAppState(
          defaultInitialState.usersRepositoryState(),
          Set.of(new SentEmail("mariah.hairam@example.com", subject, body))
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherId(){
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";

    // Act
    try {
      app.sendEmailToTeacherId(id, subject, body);

    // Assert
    } catch (Exception e) {
      Assertions.fail();
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            Set.of(new SentEmail("mariah.hairam@example.com", subject, body))
    );
    assertEquals(expectedFinalState, getFinalState());
  }
  @Test
  public void testSendEmailToNonexistentId() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "-1";
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";

    // Act
    try {
      app.sendEmailToTeacherId(id, subject, body);

    // Assert
      Assertions.fail();
    } catch (Exception e) {
      assertEquals("User " + id + " does not exist", e.getMessage());
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherUserId() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "1";
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";

    // Act
    try {
      app.sendEmailToTeacherId(id, subject, body);

    // Assert
      Assertions.fail();
    } catch (Exception e) {
      assertEquals("User " + id + " is not a teacher", e.getMessage());
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherNullSubject() {
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = null;
    final var body = "Let them students alone!!";

    try {
      app.sendEmailToTeacherId(id, subject, body);
      Assertions.fail("No exception thrown");
    } catch (Exception e) {
      assertEquals("The email subject is mandatory", e.getMessage());
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherEmptySubject() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "";
    final var body = "Let them students alone!!";

    // Act
    try {
      app.sendEmailToTeacherId(id, subject, body);

    // Assert
      Assertions.fail("No exception thrown");
    } catch (Exception e) {
      assertEquals("The email subject is mandatory", e.getMessage());
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherSubjectFullOfNothing() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "    ";
    final var body = "Let them students alone!!";

    // Act
    try {
      app.sendEmailToTeacherId(id, subject, body);

    // Assert
      Assertions.fail("No exception thrown");
    } catch (Exception e) {
      assertEquals("The email subject is mandatory", e.getMessage());
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherConfirmId(){
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, true);

      // Assert
    } catch (Exception e) {
      Assertions.fail();
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            Set.of(new SentEmail("mariah.hairam@example.com", subject, body))
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherNoConfirmBodyNull() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = null;

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, false);

      // Assert
      Assertions.fail("No exception thrown");
    } catch (Exception e) {
      assertEquals(
        "No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'",
        e.getMessage()
      );
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());

  }

  @Test
  public void testSendEmailToTeacherConfirmBodyNull() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = null;
    final var sentEmail = Set.of(new SentEmail("mariah.hairam@example.com", subject, ""));

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, true);

    // Assert
    } catch (Exception e) {
      Assertions.fail("This test shouldn't raise an Exception");
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            union(defaultInitialState.sentEmails(), sentEmail)
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherConfirmBodyEmpty() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = "";

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, true);

    // Assert
      Assertions.fail("This test should raise an Exception");
    } catch (Exception e) {
      assertEquals(
              "El cuerpo debería ser nulo. Cámbielo y mantenga marcada la casilla 'Confirmar'",
              e.getMessage()
      );
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherConfirmBodyFullOfNothing() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = "    ";

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, true);

      // Assert
      Assertions.fail("This test should raise an Exception");
    } catch (Exception e) {
      assertEquals(
              "El cuerpo debería ser nulo. Cámbielo y mantenga marcada la casilla 'Confirmar'",
              e.getMessage()
      );
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());
  }

  @Test
  public void testSendEmailToTeacherNotConfirmBodyEmpty() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = "";

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, false);

      // Assert
      Assertions.fail("This test should raise an Exception");
    } catch (Exception e) {
      assertEquals(
              "El cuerpo debería ser nulo. Cámbielo y marque la casilla 'Confirmar'",
              e.getMessage()
      );
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());

  }

  @Test
  public void testSendEmailToTeacherNotConfirmBodyFullOfNothing() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = "    ";

    // Act
    try {
      app.sendEmailToTeacherIdWithConfirmation(id, subject, body, false);

      // Assert
      Assertions.fail("This test should raise an Exception");
    } catch (Exception e) {
      assertEquals(
              "El cuerpo debería ser nulo. Cámbielo y marque la casilla 'Confirmar'",
              e.getMessage()
      );
    }

    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            defaultInitialState.sentEmails()
    );
    assertEquals(expectedFinalState, getFinalState());

  }


}


