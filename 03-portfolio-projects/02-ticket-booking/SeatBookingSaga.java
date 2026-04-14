import org.springframework.stereotype.Service;

/**
 * Saga Orchestrator for handling the ticket booking process.
 * Coordinates multiple services: Booking, Payment, and Notification.
 */
@Service
public class SeatBookingSaga {

    // Dependencies to other services or message brokers
    private final SeatLockingService lockService;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepo;

    public SeatBookingSaga(SeatLockingService lockService, PaymentService paymentService, BookingRepository bookingRepo) {
        this.lockService = lockService;
        this.paymentService = paymentService;
        this.bookingRepo = bookingRepo;
    }

    /**
     * Executes the booking saga.
     */
    public boolean executeBooking(String userId, String showId, String seatId, String paymentDetails) {
        String bookingId = "BKG-" + System.currentTimeMillis();

        try {
            // Step 1: Lock the seat temporarily
            boolean locked = lockService.lockSeat(showId, seatId, bookingId);
            if (!locked) {
                return false; // Seat already taken
            }

            // Step 2: Process payment
            boolean paymentSuccess = paymentService.process(userId, paymentDetails);
            if (!paymentSuccess) {
                // Compensating Action: Release lock
                lockService.releaseSeat(showId, seatId, bookingId);
                return false;
            }

            // Step 3: Confirm booking
            bookingRepo.save(new Booking(bookingId, userId, showId, seatId, "CONFIRMED"));
            return true;

        } catch (Exception e) {
            // Error handling & Compensating Actions
            lockService.releaseSeat(showId, seatId, bookingId);
            // Initiate refund if payment had succeeded before the crash
            return false;
        }
    }
}

// Stubs for the example
class Booking {
    String id, userId, showId, seatId, status;
    public Booking(String id, String userId, String showId, String seatId, String status) {}
}
interface SeatLockingService {
    boolean lockSeat(String showId, String seatId, String bookingId);
    void releaseSeat(String showId, String seatId, String bookingId);
}
interface PaymentService {
    boolean process(String userId, String details);
}
interface BookingRepository {
    void save(Booking booking);
}
