# Taller de TDD

Queremos desarrollar una nueva funcionalidad consistente en enviar un correo electrónico a un profesor a partir de su id.

1. El driver port `CampusApp` debería disponer de un nuevo método para esta funcionalidad que reciba el `id` del profesor, el asunto y el cuerpo del mensaje.
2. Si no existe un usuario con dicho `id` se informará mediante una excepción que contenga el mensaje `"User 2466 does not exist"` (dónde `2466` es el `id` del usuario indicado en la petición).
3. Si existe un usuario con dicho `id` pero no es un profesor, se informará mediante una excepción que contenga el mensaje `"User 2466 is not a teacher"` (dónde `2466` es el `id` del usuario indicado en la petición).
4. En caso contrario, se recuperará el `email` del usuario de la base de datos y se le enviará un correo con el asunto y el cuerpo indicados.

### Happy Path (4)

Empezamos implementando el Happy Path, que sería el visto en el punto 4. 

Para ello, escribimos un test para ``CampusApp`` en ``CampusAppEndToEndTest`` que compruebe si se envía un email a un `id` de un profesor, que se haya mandado dicho email:

```
@Test
public void testSendEmailToTeacherId(){
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final var subject = "Hey! Teacher!";
    final var body = "Let them students alone!!";
    
    // Act
    app.sendEmailToTeacherId(id, subject, body);
    
    // Assert
    final var expectedFinalState = new CampusAppState(
        defaultInitialState.usersRepositoryState(),
        Set.of(new SentEmail("mariah.hairam@example.com", subject, body))
    );
    assertEquals(expectedFinalState, getFinalState());
}
```

Luego de escribir el test, empezamos a diseñar la función.

```
public void sendEmailToTeacherId(String id, String subject, String body) {
    final User user = usersRepository.getUserById(id);
    
    emailService.sendEmail(user, subject, body);
}
```

Aquí nos encontramos con el primer problema: necesitamos una función en ``UsersRepository`` pero dicha función no existe.

Empezamos el TDD de la función de ``UsersRepository`` llamada `getUserById`, donde damos el ``id`` de un ``User`` y nos retorna el `User` correspondiente de dicho ``id`` del ``UsersRepository``.

```
@Test
default void testGetUserById(){
    // Arrange
    final var repository = getRepository(defaultInitialState);
    final var id = "3";
    
    // Act
    final User resultUser = repository.getUserById(id);
    
    // Assert
    assertEquals(mariahHairam, resultUser);
    assertExpectedFinalState(defaultInitialState);
}
```

Primero, definimos la función que deseamos en la Interface:

```
User getUserById(String id);
```

Escrito el test y con la definición de la función que queremos en la Interface de ``UsersRepository``, empezamos con la implementación en su Test Double más sencillo, ``InMemoryUsersRepository``.

```
@Override
public User getUserById(String id) {
    return state.users().stream().filter(user -> user.id().equals(id)).findFirst().get();
}
```

Comprobado que el test funciona correctamente para ``InMemoryUsersRepository``, pasamos al Test Double más complejo, ``PostgreSqlUsersRepository``.

```
@Override
public User getUserById(String id) {
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
        p(id)
    ).stream().findFirst().get();
}
```

Con los tests anteriores pasados, podemos decir que la implementación en ambos Test Double se ejecuta conforme a los requerimientos hasta el momento.

Ahora que `getUserById` es una función existente de ``UsersRepository``, podemos volver al TDD de `sendEmailToTeacherId`.

Si ejecutamos el test diseñado en un inicio, en este punto pasa el *check*, así que podemos seguir al siguiente requisito funcional.

### No existe el usuario con el id dado (2)

Empezamos escribiendo el test donde intentamos mandar un correo a un usuario inexistente.

```
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
```

Al ejecutarlo, el test falla, así que tenemos que modificar la función ``sendEmailToTeacherId`` para que lo pase.

Antes, pero, no tenemos definido qué es lo que tiene que suceder en ``UsersRepository`` cuando un ``ìd`` no existe, así que creamos tests para ello con el comportamiento esperado.

```
@Test
default void testGetUserNonExistent(){
    // Arrange
    final var repository = getRepository(defaultInitialState);
    final var id = "-1";
    
    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> {
      final User resultUser = repository.getUserById(id);
    });
    
    assertExpectedFinalState(defaultInitialState);
}
```

Ejecutando el test podemos comprobar que funciona correctamente sin hacer ningún cambio, puesto que la llamada a la función ``.get()`` dentro de ambas implementaciones ya lanza la excepción ``NoSuchElementException``. Añadimos `throws NoSuchElementException` a las implementaciones y a la Interface de UsersRepository para avisar de dicho comportamiento.

Ahora ya sí se puede pasar a la modificación del código de ``sendEmailToTeacherId`` sabiendo qué es lo que hace `getUserById` en estos casos.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    User user;
    try {
        user = usersRepository.getUserById(id);
    } catch (NoSuchElementException e) {
        throw new Exception("User " + id + " does not exist");
    }
    
    emailService.sendEmail(user, subject, body);
}
```

Con las modificaciones anteriores, el test pasa. En este punto, opinamos que el uso de capturas de excepciones para tratar con casos frecuentes es una práctica mejorable, así que decidimos refactorizar el mecanismo de excepciones a un ``Optional<User>`` cuando dicho usuario no existe.

Comentamos todo el código usado escrito hasta ahora para poder recuperarlo en caso de no lograr implementar correctamente este cambio.

Para empezar con el cambio a `Optional<User>`, reescribimos el test del Happy Path de ``UsersRepository``.

```
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
```

Comprobamos que la implementación actual devuelve un ``User`` y no un ``Optional<User>`` y falla el test, así que hacemos los cambios pertinentes en la implementación para que devuelva un ``Optional<User>``.

```
@Override
public Optional<User> getUserById(String id) {
    return state.users().stream().filter(user -> user.id().equals(id)).findFirst();
}
```

La implementación en ``InMemoryUsersRepository`` anterior pasa los tests, toca cambiar la implementación de `PostgreSqlUsersRepository`.

```
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
        p(id)
    ).stream().findFirst();
}
```

Y con la refactorización hecha en ```PostgreSqlRepository``` pasando los tests, procedemos a hacer los cambios en el test en que el `User` no existe.

```
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
```

El refactoring hecho también pasa el test del ``User`` que no existe sin requerir de más cambios, así que damos la nueva implementación con `Optional<User>` como correcta.

Ahora tenemos que ver los tests de `CampusAppEndToEndTests` si pasan con los cambios actuales. Empezamos con el Happy Path.

Al ejecutar el test, este falla. Hay que hacer cambios en `sendEmailToTeacherId` para que pase. El resultado es el siguiente: 

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    final User = usersRepository.getUserById(id).get();
    emailService.sendEmail(user, subject, body);
}
```

Con los cambios anteriores, el test del Happy Test pasa, pero el test que comprueba los casos en que el usuario no existe no. Tenemos que hacer más cambios en `sendEmailToTeacherId` para que pase este test.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {    
    User user;
    try {
        user = usersRepository.getUserById(id).get();
    } catch (NoSuchElementException e) {
        throw new Exception("User " + id + " does not exist");
    }
    
    emailService.sendEmail(user, subject, body);
}
```

El cambio anterior pasa el test que comprueba qué pasa cuando el usuario no existe, pero sigue usando la captura de excepciones para funcionar que era lo que estábamos intentando evitar con los `Optional<User>`. Hacemos un refactoring para que no haya captura de excepciones.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {    
    final Optional<User> maybeUser = usersRepository.getUserById(id);
    
    if (maybeUser.isEmpty()) {
        throw new Exception("User " + id + " does not exist");
    }
    
    emailService.sendEmail(maybeUser.get(), subject, body);
}
```

Con el cambio anterior, pasan los tests y no tenemos captura de excepciones en el código, así que procedemos al siguiente requisito funcional.

También podemos eliminar el código previamente comentado, puesto que la nueva implementación funciona como se espera.

### El id dado no es de un profesor (3)

Con este nuevo requerimiento en mente, diseñamos un test que lo represente.

```
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
```

Ejecutamos el test para ver si la implementación previa cumple con este requisito. Como el test falla, hay que realizar cambios en la función `sendEmailToTeacherId`.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
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
```

Con los cambios realizados, comprobamos que pase el nuevo test diseñado. Al pasarlo, ejecutamos todos los tests para asegurar que no ha habido ninguna regresión y todos dan resultado positivo. La implementación actual cubre todos los requisitos funcionales descritos.

## Requisitos no funcionales:

- Cualquier línea de código que se cambie o añada deberá estar cubierta por las pruebas ya existentes o por nuevas pruebas.
- Para centrarnos en TDD, en esta práctica se usarán las estrategias implementadas por `CampusAppEndToEndTest` y `PostgreSqlUsersRepositoryTest` como puntos de partida. Si queréis usar otra estrategia de testing de las explicadas debéis consultar antes con el profesor.

## Mejora 1

Después de unas pruebas de usabilidad nos indican que sucede a menudo que el usuario olvida indicar el asunto del mensaje. Queremos implementar esta modificación:

El método de envío de un email a un profesor comprobará que el asunto del mensaje no sea `null`, ni un `String` vacío ni un `String` con solo carácteres en blanco. Si tal fuera el caso, se indicará con un mensaje de error `"The email subject is mandatory"`.

Extendiendo la función diseñada en el ejercicio principal, queremos implementar tres nuevos requisitos:

1. Si el asunto es `null`, se manda el mensaje de error `"The email subject is mandatory"`
2. Si el asunto es un `String` vacío, se manda el mensaje de error `"The email subject is mandatory"`
3. Si el asunto es un `String` con solo carácteres en blanco, se manda el mensaje de error `"The email subject is mandatory"`

### Asunto nulo (1)

Empezamos diseñando el test para el comportamiento de la función ante un asunto nulo.

```
@Test
public void testSendEmailToTeacherNullSubject() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = null;
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
```

Lanzándolo, el test falla. Habrá que realizar cambios en `sendEmailToTeacherId` para que se adecúe a este nuevo comportamiento. 

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    if (subject == null) {
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
```

Ejecutando el test con los cambios anteriores, pasa el test. Ejecutamos todo el set de tests diseñados hasta ahora para asegurarnos que no ha habido ningún tipo de regresión. Todos los tests dan un resultado positivo, podemos seguir con los siguientes cambios.

### Asunto vacío (2)

Empezamos diseñando el test para el comportamiento de la función ante un asunto vacío.

```
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
```

Lanzándolo, el test falla. Habrá que realizar cambios en `sendEmailToTeacherId` para que se adecúe a este nuevo comportamiento.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    if (subject == null) {
        throw new Exception("The email subject is mandatory");
    }
    
    if (subject.isEmpty()) {
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
```

Ejecutando el test con los cambios anteriores, pasa el test. Ejecutamos todo el set de tests diseñados hasta ahora para asegurarnos que no ha habido ningún tipo de regresión. Todos los tests dan un resultado positivo, pero viendo el código podemos ver una repetición de intenciones. Refactorizamos el código para eliminar dicha repetición.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    if (subject == null || subject.isEmpty()) {
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
```

Lanzamos de nuevo todos los tests para comprobar que no ha habido ninguna regresión con este cambio. Con todos los tests pasados, procedemos al siguiente punto.

### Asunto lleno de carácteres blancos (3)

Empezamos diseñando el test para el comportamiento de la función ante un asunto vacío.

```
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
```

Lanzándolo, el test falla. Habrá que realizar cambios en `sendEmailToTeacherId` para que se adecúe a este nuevo comportamiento.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    if (subject == null || subject.isEmpty()) {
        throw new Exception("The email subject is mandatory");
    }
    
    if (subject.strip().isEmpty()) {
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
```

Ejecutando el test con los cambios anteriores, pasa el test. Ejecutamos todo el set de tests diseñados hasta ahora para asegurarnos que no ha habido ningún tipo de regresión. Todos los tests dan un resultado positivo, pero viendo el código podemos ver una repetición de intenciones. Refactorizamos el código para eliminar dicha repetición.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    if (subject == null || subject.isEmpty() || subject.strip().isEmpty()) {
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
```

Lanzamos de nuevo todos los tests para comprobar que no ha habido ninguna regresión con este cambio. Aún con estos cambios hechos, IntelliJ se queja de que podemos sustituir un fragmento del código por otro que considera más correcto, cambiando `.strip().isEmpty()` por `.isBlank()`.

```
public void sendEmailToTeacherId(String id, String subject, String body) throws Exception {
    if (subject == null || subject.isEmpty() || subject.isBlank()) {
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
```

Los tests de nuevo vuelven a pasar con este nuevo cambio. Se nos ocurre otra idea, `.isBlank()` también debería incluir `.isEmpty()`, así que debería ser posible eliminar uno de los `OR` de la condición.

```
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
```

De nuevo, ejecutamos los tests para comprobar que no ha habido ninguna regresión y el resultado es positivo. Con todos los tests pasados, podemos dar estos requisitos funcionales como implementados satisfactoriamente.

## Mejora 2

Desde el análisis de la usabilidad nos dicen que, de nuevo, han detectado una posible mejora. Aunque sea opcional, en ocasiones el usuario olvida indicar el cuerpo del mensaje. Queremos implementar esta modificación:

1. Añadiremos un parámetro booleano llamado `confirm` (confirmar).

2. Si el parámetro `confirm` es `false` y el cuerpo del mensaje es `null`, se informará mediante una excepción que contenga el mensaje `"No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'"`. Si el parámetro `confirm` es `true` se enviará el mensaje sin cuerpo. Para evitar confusiones, si el cuerpo del mensaje a enviar es un `String` vacío o un `String` con solo carácteres en blanco, se informará como error con un mensaje que distinga entre ambos casos, independientemente del valor del parámetro `confirm`; es decir, si no se quiere enviar cuerpo del mensaje, deberá indicarse con `null` Y con el parámetro `confirm` a `true`.

### Happy path

Definiremos el siguiente test como prototipo del resultado esperado de la función. Aquí definiremos el método `sendEmailToTeacherIdWithConfirmation(...)`.

```
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

```

Para añadir este nuevo requisito, creamos una nueva función como wrapper de la función anterior, donde además de los parámetros de `id`, `subject` y `body` se le pasa un cuarto parámetro `confirm`. De esta forma, tenemos tanto la funcionalidad anterior como la nueva deseada sin romper nada.

```
public void sendEmailToTeacherIdWithConfirmation(String id, String subject, String body, boolean confirm) throws Exception {
    sendEmailToTeacherId(id, subject, body);
}
```

Con este cambio, podemos ejecutar los test y ver que pasan correctamente. Pero nos faltará definir los casos excepcionales.

Para cumplir con la tarea, dividiremos este requisito en 4 pasos

1. Cuerpo `null` y `confirm` es `false`: informar con `Exception("No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'")`
2. Cuerpo `null` y `confirm` es `true`: mandar correo sin cuerpo
3. Cuerpo vacío o solo carácteres en blanco y `confirm` es `true`: informar con `Exception("El cuerpo debería ser nulo. Cámbielo y mantenga marcada la casilla 'Confirmar'")`
4. Cuerpo vacío o solo carácteres en blanco y `confirm` es `false`: informar de error `Exception("El cuerpo debería ser nulo. Cámbielo y marque la casilla 'Confirmar'")`

### Cuerpo Nulo + Confirm Falso (1)

Empezamos definiendo el test que describe el primer fragmento del requisito.

```
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
```

Ejecutando el test, este falla. Modificamos la nueva función creada para que pase el test.

```
public void sendEmailToTeacherIdWithConfirmation(String id, String subject, String body, boolean confirm) throws Exception {
    if (!confirm && body == null) {
        throw new Exception("No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'");
    }

    sendEmailToTeacherId(id, subject, body);
}
```

Con los cambios hechos, re-ejecutamos el test con un resultado positivo. Ejecutamos todos los tests para asegurar que no ha habido ninguna regresión, y al confirmar que todos tienen resultados positivos pasamos al siguiente punto.

### Cuerpo Nulo + Confirm Falso (2)


Empezamos definiendo el test que describe el segundo fragmento del requisito.

```
Test
public void testSendEmailToTeacherConfirmBodyNull() {
    // Arrange
    final var app = getApp(defaultInitialState);
    final var id = "3";
    final String subject = "Hey! Teacher!";
    final String body = null;
    
    // Act
    try {
        app.sendEmailToTeacherIdWithConfirmation(id, subject, body, true);
    
    // Assert
    } catch (Exception e) {
        Assertions.fail("This test shouldn't raise an Exception");
    }
    
    final var sentEmail = Set.of(new SentEmail("mariah.hairam@example.com", subject, ""));
    final var expectedFinalState = new CampusAppState(
        defaultInitialState.usersRepositoryState(),
        union(defaultInitialState.sentEmails(), sentEmail)
    );
    assertEquals(expectedFinalState, getFinalState());
}
```

Ejecutando el test en este punto vemos que falla. Hay que hacer cambios en la función para que pase este test.

```
public void sendEmailToTeacherIdWithConfirmation(String id, String subject, String body, boolean confirm) throws Exception {
    if (!confirm && body == null) {
        throw new Exception("No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'");
    }
    
    if (confirm && body == null) {
        body = "";
    }
    
    sendEmailToTeacherId(id, subject, body);
}
```

Tras aplicar este cambio, podemos observar como pasa el test correctamente y podemos pasar a implementar la prueba relacionada con el punto 3.

### Cuerpo Vacío + Confirm True (3)

Empezaremos a implementar la prueba del punto 3 asegurándonos que la excepción contenga el mensaje esperado.

```
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
```

Ejecutamos el test y podemos observar que no pasa. Nos tocará añadir un cambio al método `sendEmailToTeacherIdWithConfirmation(...)` de la clase `CampusApp` para controlar el caso en que el string está vacío y el confirm está a `true`.

```
public void sendEmailToTeacherIdWithConfirmation(String id, String subject, String body, boolean confirm) throws Exception {
    if (!confirm && body == null) {
        throw new Exception("No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'");
    }
    
    if (confirm && body == null) {
        body = "";
    }
    
    if (confirm && body.isBlank()) {
        throw new Exception("El cuerpo debería ser nulo. Cámbielo y mantenga marcada la casilla 'Confirmar'");
    }
    
    sendEmailToTeacherId(id, subject, body);
}
```

Tras aplicar este cambio, hemos visto que el test pasaba correctamente pero nos hemos dado cuenta de que había mucho codigo duplicado. Entramos en la fase de refactor y aplicamos el siguiente cambio:
```
public void sendEmailToTeacherIdWithConfirmation(String id, String subject, String body, boolean confirm) throws Exception {

    if (confirm) {
        if (body == null) {
            body = "";
        } else if (body.isBlank()) {
            throw new Exception("El cuerpo debería ser nulo. Cámbielo y mantenga marcada la casilla 'Confirmar'");
        }
    } else if (body == null) {
        throw new Exception("No se ha indicado el cuerpo del mensaje. Infórmelo o marque la casilla 'Confirmar'");
    }
    
    sendEmailToTeacherId(id, subject, body);
}
```
Una vez aplicado el cambio, vemos como pasa el test correctamente y podemos pasar a la implementación del siguiente caso.

### Cuerpo Vacío (carácteres en blanco) + Confirm True (3.1)

Implementaremos el siguiente test para tener en cuenta el caso de carácteres en blanco.
```
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
```

Con la implementación actual podemos observar como pasan los test correctamente, asi que podemos pasar a implementar el siguiente caso.

### Cuerpo Vacío + Confirm False (4)

Implementamos el test en relación al caso definido y lo ejecutamos.

```
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
```

Ejecutamos el test y podemos observar que no pasa. Nos tocará añadir un cambio al método `sendEmailToTeacherIdWithConfirmation(...)` de la clase `CampusApp` para controlar el caso en que el string está vacío y el confirm está a `false`, para devolver el mensaje correcto en la excepción.

```
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
```

Una vez hemos añadido este cambio, ejecutamos una vez más los test y vemos que pasan correctamente.

### Cuerpo Vacío (carácteres en blanco) + Confirm False (4.1)

Definiremos el test para este último caso, teniendo en cuenta el parámetro `body` lleno de carácteres en blanco.

```
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
```

Podemos observar como este test pasa correctamente sin necesidad de implementar ningún cambio. Con esto habremos terminado nuestro desarrollo.
