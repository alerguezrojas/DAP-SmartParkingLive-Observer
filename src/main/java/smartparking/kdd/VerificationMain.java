package smartparking.kdd;

import smartparking.kdd.model.KddEvent;
import smartparking.kdd.model.KddUser;
import smartparking.kdd.model.Location;
import smartparking.kdd.service.KddEventRepository;
import smartparking.kdd.service.KddService;
import smartparking.kdd.service.KddUserRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerificationMain {

    public static void main(String[] args) {
        System.out.println("=== Iniciando Verificación ===");

        // 1. Crear Mocks (Proxies)
        Map<String, KddUser> userDb = new HashMap<>();
        Map<String, KddEvent> eventDb = new HashMap<>();

        KddUserRepository userRepo = (KddUserRepository) Proxy.newProxyInstance(
                VerificationMain.class.getClassLoader(),
                new Class[] { KddUserRepository.class },
                new InMemoryRepoHandler(userDb, KddUser.class));

        KddEventRepository eventRepo = (KddEventRepository) Proxy.newProxyInstance(
                VerificationMain.class.getClassLoader(),
                new Class[] { KddEventRepository.class },
                new InMemoryRepoHandler(eventDb, KddEvent.class));

        // 2. Instanciar Servicio
        KddService service = new KddService(userRepo, eventRepo);

        // 3. Escenario de Prueba
        try {
            // a) Registrar Mod en (0,0)
            System.out.println("\n1. Registrando Mod...");
            KddUser mod = service.registerUser("ModAlex", 0.0, 0.0, true);
            System.out.println("Mod registrado: " + mod.getName() + " en " + mod.getLocation().getLat() + ","
                    + mod.getLocation().getLon());

            // b) Registrar Usuario Lejos (20,20) - Distancia > 10km
            // 1 grado lat ~= 111km. 20 grados es muy lejos.
            System.out.println("\n2. Registrando Usuario Lejos...");
            KddUser user = service.registerUser("UserPepe", 20.0, 20.0, false);

            // Configurar callback para verificar notificaciones
            AtomicBoolean receivedNotification = new AtomicBoolean(false);
            user.setNotificationCallback(evt -> {
                System.out.println("!!! NOTIFICACIÓN RECIBIDA: " + evt.getName());
                receivedNotification.set(true);
            });

            // Verificar que NO está suscrito (hack: accediendo a campo privado o confiando
            // en lógica)
            // Como no podemos acceder a subscribers fácilmente desde fuera sin getter (si
            // no lo añadí),
            // verificaremos indirectamente creando un evento.

            System.out.println("Creando evento por Mod (Usuario lejos)...");
            service.createEvent("Evento 1", "Desc", 0.0, 0.0, mod.getId());

            if (receivedNotification.get()) {
                System.err.println("FALLO: El usuario recibió notificación estando lejos.");
            } else {
                System.out.println("ÉXITO: Usuario lejos no recibió notificación.");
            }

            // c) Mover Usuario Cerca (0.05, 0.05) - Distancia < 10km
            // 0.05 grados es aprox 5.5km
            System.out.println("\n3. Moviendo Usuario Cerca...");
            service.updateUserLocation(user.getId(), 0.05, 0.05);

            // d) Mod crea otro evento
            System.out.println("Creando evento por Mod (Usuario cerca)...");
            service.createEvent("Evento 2", "Desc", 0.0, 0.0, mod.getId());

            if (receivedNotification.get()) {
                System.out.println("ÉXITO: Usuario cerca recibió notificación.");
            } else {
                System.err.println("FALLO: El usuario NO recibió notificación estando cerca.");
            }

            // e) Mover Usuario Lejos de nuevo
            System.out.println("\n4. Moviendo Usuario Lejos de nuevo...");
            receivedNotification.set(false);
            service.updateUserLocation(user.getId(), 20.0, 20.0);

            // f) Mod crea tercer evento
            System.out.println("Creando evento por Mod (Usuario lejos de nuevo)...");
            service.createEvent("Evento 3", "Desc", 0.0, 0.0, mod.getId());

            if (receivedNotification.get()) {
                System.err.println("FALLO: El usuario recibió notificación tras alejarse.");
            } else {
                System.out.println("ÉXITO: Usuario lejos no recibió notificación.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handler genérico para simular JpaRepository básico
    static class InMemoryRepoHandler implements InvocationHandler {
        private final Map<String, Object> db;
        private final Class<?> entityType;

        public InMemoryRepoHandler(Map db, Class<?> entityType) {
            this.db = db;
            this.entityType = entityType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if (name.equals("save")) {
                Object entity = args[0];
                Method getId = entityType.getMethod("getId");
                String id = (String) getId.invoke(entity);
                db.put(id, entity);
                return entity;
            } else if (name.equals("findById")) {
                String id = (String) args[0];
                return Optional.ofNullable(db.get(id));
            } else if (name.equals("findAll")) {
                return new ArrayList<>(db.values());
            } else if (name.equals("findByName")) {
                // Implementación simple para KddUserRepository
                String searchName = (String) args[0];
                Method getName = entityType.getMethod("getName");
                for (Object obj : db.values()) {
                    String objName = (String) getName.invoke(obj);
                    if (objName.equals(searchName))
                        return Optional.of(obj);
                }
                return Optional.empty();
            }

            return null;
        }
    }
}
