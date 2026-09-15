
import java.time.LocalDate;
import com.verisure.backend.entity.enums.ActivityStatus;


public record ActivityCardResponse(
        Long id,
        String title,
        String partnerName,
        String line,
        String mode,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        Integer hours,
        Integer spots,
        Integer occupiedSpots,
        String imageUrl,
        ActivityStatus status,
        boolean favoritedByMe) {
}
