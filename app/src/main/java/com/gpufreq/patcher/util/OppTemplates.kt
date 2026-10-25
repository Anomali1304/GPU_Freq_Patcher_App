package com.gpufreq.patcher.util

data class OppTemplate(
    val name: String,
    val chipset: String,
    val description: String,
    val rows: List<OppRow>
)

object OppTemplates {
    val presets = listOf(
        OppTemplate(
            name = "Dimensity 8100 (Mali-G610 MC6)",
            chipset = "MT6895 / Dimensity 8100",
            description = "High performance 8-step OPP table optimized for MediaTek Dimensity 8100 GPU (850MHz max)",
            rows = listOf(
                OppRow(850000, 85000, 87500, 0, 0, 0),
                OppRow(798000, 82500, 85000, 0, 0, 0),
                OppRow(746000, 80000, 82500, 0, 0, 0),
                OppRow(694000, 77500, 80000, 1, 0, 0),
                OppRow(632000, 75000, 77500, 1, 0, 0),
                OppRow(570000, 72500, 75000, 1, 0, 0),
                OppRow(508000, 70000, 72500, 1, 0, 0),
                OppRow(446000, 67500, 70000, 2, 0, 0)
            )
        ),
        OppTemplate(
            name = "Dimensity 9000 (Mali-G710 MC10)",
            chipset = "MT6983 / Dimensity 9000",
            description = "Aggressive 10-step OPP table for flagship Mali-G710 (950MHz max)",
            rows = listOf(
                OppRow(950000, 90000, 92500, 0, 0, 0),
                OppRow(900000, 87500, 90000, 0, 0, 0),
                OppRow(850000, 85000, 87500, 0, 0, 0),
                OppRow(800000, 82500, 85000, 0, 0, 0),
                OppRow(750000, 80000, 82500, 1, 0, 0),
                OppRow(700000, 77500, 80000, 1, 0, 0),
                OppRow(650000, 75000, 77500, 1, 0, 0),
                OppRow(600000, 72500, 75000, 1, 0, 0),
                OppRow(550000, 70000, 72500, 2, 0, 0),
                OppRow(500000, 67500, 70000, 2, 0, 0)
            )
        ),
        OppTemplate(
            name = "Helio G99 / G96 (Mali-G57 MC2)",
            chipset = "MT6789 / Helio G99",
            description = "Standard balanced OPP table for MediaTek Helio G99 (1000MHz max, 45 slots stock structure)",
            rows = listOf(
                OppRow(1000000, 85000, 95000, 0, 0, 0),
                OppRow(950000, 83125, 93125, 0, 0, 0),
                OppRow(900000, 81250, 91250, 0, 0, 0),
                OppRow(850000, 79375, 89375, 0, 0, 0),
                OppRow(800000, 77500, 87500, 1, 0, 0),
                OppRow(750000, 75625, 85625, 1, 0, 0),
                OppRow(700000, 73750, 83750, 1, 0, 0),
                OppRow(650000, 71875, 81875, 1, 0, 0),
                OppRow(600000, 70000, 80000, 1, 0, 0),
                OppRow(550000, 68125, 78125, 2, 0, 0),
                OppRow(500000, 66250, 76250, 2, 0, 0),
                OppRow(450000, 64375, 74375, 2, 0, 0),
                OppRow(400000, 62500, 72500, 2, 0, 0),
                OppRow(350000, 60625, 70625, 2, 0, 0),
                OppRow(300000, 58750, 68750, 3, 0, 0)
            )
        ),
        OppTemplate(
            name = "Mali-G715 Immortalis (Overclocked Preset)",
            chipset = "MT6985 / Dimensity 9200",
            description = "High frequency 12-step overclocked preset for Mali-G715 (1150MHz top)",
            rows = listOf(
                OppRow(1150000, 95000, 97500, 0, 0, 0),
                OppRow(1100000, 92500, 95000, 0, 0, 0),
                OppRow(1050000, 90000, 92500, 0, 0, 0),
                OppRow(1000000, 87500, 90000, 0, 0, 0),
                OppRow(950000, 85000, 87500, 0, 0, 0),
                OppRow(900000, 82500, 85000, 1, 0, 0),
                OppRow(850000, 80000, 82500, 1, 0, 0),
                OppRow(800000, 77500, 80000, 1, 0, 0),
                OppRow(750000, 75000, 77500, 1, 0, 0),
                OppRow(700000, 72500, 75000, 2, 0, 0),
                OppRow(650000, 70000, 72500, 2, 0, 0),
                OppRow(600000, 67500, 70000, 2, 0, 0)
            )
        )
    )
}
