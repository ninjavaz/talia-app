package app.talia.activity;

import app.talia.activity.model.WorkoutCategory;
import app.talia.activity.service.CalorieEstimate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** REQ-022: kcal = MET x kg x hours, MET gym 5.0, pilates 3.0, walk 3.5, other 4.0. */
class CalorieEstimateTest {

    @Test
    @DisplayName("REQ-022 the estimate uses the MET of the workout type")
    void metPerType() {
        assertThat(CalorieEstimate.kcal(WorkoutCategory.GYM, 60, new BigDecimal("64"))).isEqualTo(320);
        assertThat(CalorieEstimate.kcal(WorkoutCategory.PILATES, 50, new BigDecimal("70"))).isEqualTo(175);
        assertThat(CalorieEstimate.kcal(WorkoutCategory.WALK, 30, new BigDecimal("60"))).isEqualTo(105);
        assertThat(CalorieEstimate.kcal(WorkoutCategory.OTHER, 45, new BigDecimal("80"))).isEqualTo(240);
    }

    @Test
    @DisplayName("REQ-022 the estimate is rounded to whole kcal")
    void rounded() {
        // 3.0 x 64.3 x 0.75 = 144.675
        assertThat(CalorieEstimate.kcal(WorkoutCategory.PILATES, 45, new BigDecimal("64.3"))).isEqualTo(145);
    }

    @Test
    @DisplayName("REQ-022 without a weight there is no estimate")
    void noWeight() {
        assertThat(CalorieEstimate.kcal(WorkoutCategory.GYM, 60, null)).isNull();
    }
}
