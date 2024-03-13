package edu.upc.talent.swqa.campus.infrastructure.test;

import edu.upc.talent.swqa.campus.domain.User;
import edu.upc.talent.swqa.campus.domain.UsersRepository;
import edu.upc.talent.swqa.campus.test.utils.TestFixtures;
import static edu.upc.talent.swqa.campus.test.utils.TestFixtures.janeDoe;
import static edu.upc.talent.swqa.campus.test.utils.TestFixtures.johnDoe;
import static edu.upc.talent.swqa.campus.test.utils.TestFixtures.mariahHairam;
import edu.upc.talent.swqa.campus.test.utils.UsersRepositoryState;
import static edu.upc.talent.swqa.test.utils.Asserts.assertEquals;
import static edu.upc.talent.swqa.util.Utils.now;
import static edu.upc.talent.swqa.util.Utils.plus;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public interface UsersRepositoryTest {

  UsersRepository getRepository(final UsersRepositoryState initialState);

  UsersRepositoryState getFinalState();


  default void assertExpectedFinalState(final UsersRepositoryState expectedFinalState) {
    assertEquals(expectedFinalState, getFinalState());
  }

  UsersRepositoryState defaultInitialState = TestFixtures.defaultInitialState.usersRepositoryState();

  @Test
  default void testGetUsersByGroup() {
    final var repository = getRepository(defaultInitialState);
    final var actual = repository.getUsersByGroup("swqa");
    assertEquals(Set.of(johnDoe, janeDoe, mariahHairam), new HashSet<>(actual));
    assertExpectedFinalState(defaultInitialState);
  }

  @Test
  @Disabled
  default void testCreateUser() {
    // Arrange
    final var repository = getRepository(defaultInitialState);
    final var id = "0";
    final var name = "Jack";
    final var surname = "Doe";
    final var email = "jack.doe@example.com";
    final var role = "student";
    final var groupName = "swqa";
    final var now = now();
    // Act
    repository.createUser(id, name, surname, email, role, groupName);
    // Assert
    final var expectedNewUser = new User(id, name, surname, email, role, groupName, now);
    final var expected =
          new UsersRepositoryState(plus(defaultInitialState.users(), expectedNewUser), defaultInitialState.groups());
    assertExpectedFinalState(expected);
  }

  @Test
  @Disabled
  default void testCreateUserFailsIfGroupDoesNotExist() {
    final var repository = getRepository(defaultInitialState);
    final var groupName = "non-existent";
    final var exception = assertThrows(RuntimeException.class, () ->
          repository.createUser("0", "a", "b", "a.b@example.com", "student", groupName)
    );
    assertEquals("Group " + groupName + " does not exist", exception.getMessage());
    assertExpectedFinalState(defaultInitialState);
  }

  @Test
  default void testGetUserById(){
    // Arrange
    final var repository = getRepository(defaultInitialState);
    final var id = "3";

    // Act
    final Optional<User> resultUser = repository.getUserById(id);

    // Assert
    assertEquals(true, resultUser.isPresent());
    assertEquals(mariahHairam, resultUser.get());
    assertExpectedFinalState(defaultInitialState);
  }

  @Test
  default void testGetUserNonExistent(){
    // Arrange
    final var repository = getRepository(defaultInitialState);
    final var id = "-1";

    // Act
    final Optional<User> resultUser = repository.getUserById(id);

    // Assert
    assertEquals(true, resultUser.isEmpty());
    assertExpectedFinalState(defaultInitialState);
  }
}



