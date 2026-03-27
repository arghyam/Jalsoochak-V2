package org.arghyam.jalsoochak.tenant.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TenantConfigKeyEnum – isPublic contract")
class TenantConfigKeyEnumTest {

    @Test
    void publicKeys_containExpectedEntries() {
        List<TenantConfigKeyEnum> publicKeys = Arrays.stream(TenantConfigKeyEnum.values())
                .filter(TenantConfigKeyEnum::isPublic)
                .toList();

        assertThat(publicKeys).containsExactlyInAnyOrder(
                TenantConfigKeyEnum.AVERAGE_MEMBERS_PER_HOUSEHOLD,
                TenantConfigKeyEnum.WATER_NORM,
                TenantConfigKeyEnum.DATE_FORMAT_SCREEN,
                TenantConfigKeyEnum.DATE_FORMAT_TABLE,
                TenantConfigKeyEnum.SUPPORTED_LANGUAGES,
                TenantConfigKeyEnum.DISPLAY_DEPARTMENT_MAPS
        );
    }

    @Test
    void sensitiveKeys_areNotPublic() {
        assertThat(TenantConfigKeyEnum.MESSAGE_BROKER_CONNECTION_SETTINGS.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.STATE_IT_SYSTEM_CONNECTION.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.GLIFIC_MESSAGE_TEMPLATES.isPublic()).isFalse();
    }

    @Test
    void operationalKeys_areNotPublic() {
        assertThat(TenantConfigKeyEnum.PUMP_OPERATOR_REMINDER_NUDGE_TIME.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.FIELD_STAFF_ESCALATION_RULES.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.DATA_CONSOLIDATION_TIME.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.STATE_DATA_RECONCILIATION_TIME.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.METER_CHANGE_REASONS.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.LOCATION_CHECK_REQUIRED.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.TENANT_SUPPORTED_CHANNELS.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.TENANT_WATER_QUANTITY_SUPPLY_THRESHOLD.isPublic()).isFalse();
        assertThat(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON.isPublic()).isFalse();
    }
}
