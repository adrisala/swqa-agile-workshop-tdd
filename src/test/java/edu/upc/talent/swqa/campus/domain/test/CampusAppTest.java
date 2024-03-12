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
  @Disabled
  public void testSendEmailToTeacherId(){
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";

    app.sendEmailToTeacherId(id, subject, body);
    final var expectedFinalState = new CampusAppState(
            defaultInitialState.usersRepositoryState(),
            Set.of(new SentEmail("mariah.hairam@example.com", subject, body))
    );
    assertEquals(expectedFinalState, getFinalState());
  }
  @Test
  public void testFailIfIdNotExist() {

  }

}
