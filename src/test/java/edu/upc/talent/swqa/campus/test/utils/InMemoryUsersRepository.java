package edu.upc.talent.swqa.campus.test.utils;

import edu.upc.talent.swqa.campus.domain.BirthdayEmailData;
import edu.upc.talent.swqa.campus.domain.User;
import edu.upc.talent.swqa.campus.domain.UsersRepository;
import static edu.upc.talent.swqa.util.Utils.now;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record InMemoryUsersRepository(UsersRepositoryState state) implements UsersRepository {

  @Override
  public void createUser(
        final String id,
        final String name,
        final String surname,
        final String email,
        final String role,
        final String groupName
  ) {
    final var user = new User(id, name, surname, email, role, groupName, now());
    state.users().add(user);
  }

  @Override
  public void createGroup(final String id, final String name) {
    state.groups().add(new Group(id, name));
  }

  @Override
  public Optional<User> getUserById(String id) {
    return state.users().stream().filter(user -> user.id().equals(id)).findFirst();
  }


  @Override
  public List<User> getUsersByGroup(final String groupName) {
    return state.users().stream()
                .filter(user -> user.groupName().equals(groupName))
                .toList();
  }

  @Override public List<BirthdayEmailData> getUsersInBirthday() {
    final var today = LocalDate.now();
    return state.users().stream()
                .filter(user -> {
                  final var createdDate = user.createdDate();
                  return createdDate.getDayOfMonth() == today.getDayOfMonth() &&
                         createdDate.getMonth() == today.getMonth();
                })
                .map(user -> new BirthdayEmailData(
                      user.email(),
                      user.name(),
                      user.surname(),
                      user.createdAt().toString()
                ))
                .toList();
  }
}
