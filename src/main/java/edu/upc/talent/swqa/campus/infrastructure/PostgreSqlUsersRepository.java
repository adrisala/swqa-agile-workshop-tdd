package edu.upc.talent.swqa.campus.infrastructure;

import edu.upc.talent.swqa.campus.domain.BirthdayEmailData;
import edu.upc.talent.swqa.campus.domain.User;
import edu.upc.talent.swqa.campus.domain.UsersRepository;
import edu.upc.talent.swqa.jdbc.Database;
import static edu.upc.talent.swqa.jdbc.Param.p;

import java.util.List;
import java.util.Optional;

public class PostgreSqlUsersRepository implements UsersRepository {

  private final Database db;

  public PostgreSqlUsersRepository(final Database db) {
    this.db = db;
  }

  public List<User> getUsersByGroup(final String groupName) {
    return db.select(
          """
          select u.id, u.name, u.surname, u.email, u.role, u.created_at
          from users u join groups g on u.group_id = g.id
          where u.active and g.name = ?""",
          (rs) -> new User(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                groupName,
                rs.getInstant(6)
          ),
          p(groupName)
    );
  }


  @Override public List<BirthdayEmailData> getUsersInBirthday() {
    return db.select(
          "select email, name, surname, created_at from users where (now() - created_at) > '1 days' and  to_char(created_at, 'MM-dd') = to_char(now(), 'MM-dd')",
          (rs) -> new BirthdayEmailData(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4))
    );
  }

  @Override public void createUser(
        final String id,
        final String name,
        final String surname,
        final String email,
        final String role,
        final String groupName
  ) {
    db.update(
          "insert into users (id, name, surname, email, role, group_id) " +
          "values (?, ?, ?, ?, ?, (select id from groups where name = ?))",
          p(id),
          p(name),
          p(surname),
          p(email),
          p(role),
          p(groupName)
    );
  }

  @Override public void createGroup(final String id, final String name) {
    db.update("insert into groups (id, name) values (?, ?)", p(id), p(name));
  }

  @Override
  public Optional<User> getUserById(String id) {

    return db.select("""
          select u.id, u.name, u.surname, u.email, u.role, g.name, u.created_at
          from users u join groups g on u.group_id = g.id
          where u.active and u.id = ?""",
          (u) -> new User(
                  u.getString(1),
                  u.getString(2),
                  u.getString(3),
                  u.getString(4),
                  u.getString(5),
                  u.getString(6),
                  u.getInstant(7)
          ),
          p(id)).stream().findFirst();
  }
}
