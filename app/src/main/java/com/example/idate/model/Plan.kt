package com.example.idate.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.idate.R

data class Plan(
    val id: Int,
    val category: String,       // e.g. "Planes", "Comida", "Película", "Fiesta", "Juegos", "Aire Libre", "Música"
    val title: String,          // e.g. "Armar Legos", "Sushi", "Babylon"
    val detail: String,         // e.g. "Casa / Café", "$200-300", "3h 9m"
    @DrawableRes val imageResId: Int = R.drawable.plan_legos,
    val imageUrl: String = "",
    val icon: ImageVector = Icons.Default.Celebration,
    val iconEmoji: String = "🎉",
    val description: String = "",
    val location: String = "",
    val duration: String = "",
    val budget: String = "",
    val tags: List<String> = emptyList(),
    val categoryColor: Color = Color.Red,
    val targetFriendId: String? = null,
    val targetGroupId: String? = null,
    val targetFriendName: String? = null,
    val targetGroupName: String? = null,
    val scope: PlanScope = PlanScope.GLOBAL
)

object SamplePlans {
    val defaultPlans = listOf(
        Plan(
            id = 1,
            category = "Planes",
            title = "Armar Legos",
            detail = "Casa / Café",
            imageResId = R.drawable.plan_legos,
            icon = Icons.Default.Extension,
            iconEmoji = "🧩",
            description = "Compra un set de Lego botánico o mini figuras, pide dos cafés calientes y pasen la tarde construyendo juntos.",
            location = "Casa / Café de Especialidad",
            duration = "2 - 3 horas",
            budget = "$200 - $350 MXN",
            tags = listOf("Creativo", "Tranquilo", "Café"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 2,
            category = "Comida",
            title = "Sushi & Sake",
            detail = "$200-300",
            imageResId = R.drawable.plan_sushi,
            icon = Icons.Default.Restaurant,
            iconEmoji = "🍣",
            description = "Degustación de rollos artesanales, nigiris y tabla de gyozas con sake o cócteles japoneses.",
            location = "Restaurante Omakase / Terraza",
            duration = "2 horas",
            budget = "$200 - $300 MXN",
            tags = listOf("Gourmet", "Japonés", "Cena"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 3,
            category = "Película",
            title = "Babylon",
            detail = "3h 9m",
            imageResId = R.drawable.plan_cine,
            icon = Icons.Default.Movie,
            iconEmoji = "🎬",
            description = "Noche de cine épico con palomitas extra mantequilla, nachos y refresco en sala VIP.",
            location = "Cine VIP Centro",
            duration = "3 horas 9 min",
            budget = "$180 MXN",
            tags = listOf("Cine", "Palomitas", "Entretenimiento"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 4,
            category = "Fiesta",
            title = "Noche de Drinks",
            detail = "Terraza / 10pm",
            imageResId = R.drawable.plan_drinks,
            icon = Icons.Default.LocalBar,
            iconEmoji = "🍹",
            description = "Cócteles de autor, música en vivo con DJ local y el mejor ambiente para bailar con amigos.",
            location = "Rooftop Bar Zona Rosa",
            duration = "Toda la noche",
            budget = "$300 - $500 MXN",
            tags = listOf("Fiesta", "Drinks", "Música"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 5,
            category = "Juegos",
            title = "Escape Room",
            detail = "$250 MXN",
            imageResId = R.drawable.plan_escape,
            icon = Icons.Default.Key,
            iconEmoji = "🗝️",
            description = "Resuelvan acertijos y escapen de la habitación temática antes de que se agoten los 60 minutos.",
            location = "Escape Room Centro",
            duration = "1.5 horas",
            budget = "$250 MXN / persona",
            tags = listOf("Misterio", "Trabajo en Equipo", "Adrenalina"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 6,
            category = "Fiesta",
            title = "Boliche Neón",
            detail = "Bowling Club",
            imageResId = R.drawable.plan_boliche,
            icon = Icons.Default.Sports,
            iconEmoji = "🎳",
            description = "Luces de neón, alitas picantes, boliche competitivo y jarras de cerveza bien fría.",
            location = "Boliche Neón",
            duration = "2.5 horas",
            budget = "$200 MXN / persona",
            tags = listOf("Boliche", "Competencia", "Fiesta"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 7,
            category = "Aire Libre",
            title = "Picnic al Parque",
            detail = "Parque Central",
            imageResId = R.drawable.plan_picnic,
            icon = Icons.Default.Park,
            iconEmoji = "🧺",
            description = "Manta grande, canasta de frutas, botanas y proyección de película al aire libre.",
            location = "Parque Metropolitano",
            duration = "3 horas",
            budget = "$100 - $200 MXN",
            tags = listOf("Picnic", "Romántico", "Aire Libre"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 8,
            category = "Comida",
            title = "Tacos & Chelas",
            detail = "$150 MXN",
            imageResId = R.drawable.plan_tacos,
            icon = Icons.Default.Fastfood,
            iconEmoji = "🌮",
            description = "Ruta por los mejores 3 puestos de tacos al pastor y suadero con cervezas artesanales.",
            location = "Zona Gastronómica",
            duration = "2 horas",
            budget = "$150 MXN",
            tags = listOf("Tacos", "Callejeros", "Amigos"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 9,
            category = "Comida",
            title = "Pizza & Vino",
            detail = "$250 MXN",
            imageResId = R.drawable.plan_pizza,
            icon = Icons.Default.LocalPizza,
            iconEmoji = "🍕",
            description = "Pizza artesanal en horno de piedra con copa de vino tinto y ensalada fresca.",
            location = "Trattoria Italiana",
            duration = "2 horas",
            budget = "$250 MXN",
            tags = listOf("Pizza", "Vino", "Italiano"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 10,
            category = "Aire Libre",
            title = "Senderismo",
            detail = "Mirador / 7am",
            imageResId = R.drawable.plan_senderismo,
            icon = Icons.Default.Terrain,
            iconEmoji = "⛰️",
            description = "Subida a la montaña temprano, fotos espectaculares al amanecer y café caliente en la cima.",
            location = "Parque Nacional",
            duration = "3.5 horas",
            budget = "Gratis",
            tags = listOf("Naturaleza", "Ejercicio", "Amanecer"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 11,
            category = "Música",
            title = "Noche de Jazz",
            detail = "Jazz Club",
            imageResId = R.drawable.plan_jazz,
            icon = Icons.Default.MusicNote,
            iconEmoji = "🎷",
            description = "Concierto íntimo de jazz en vivo, copas de martini y ambiente relajante con luces suaves.",
            location = "Jazz & Cocktail Club",
            duration = "2.5 horas",
            budget = "$300 MXN",
            tags = listOf("Jazz", "Música", "Relax"),
            categoryColor = Color.Red
        ),
        Plan(
            id = 12,
            category = "Planes",
            title = "Taller Cerámica",
            detail = "Estudio / 4pm",
            imageResId = R.drawable.plan_ceramica,
            icon = Icons.Default.Palette,
            iconEmoji = "🎨",
            description = "Modelar y pintar tus propias tazas o macetas de barro guiados por un ceramista local.",
            location = "Taller de Arte & Cerámica",
            duration = "2 horas",
            budget = "$280 MXN",
            tags = listOf("Arte", "Cerámica", "Manualidades"),
            categoryColor = Color.Red
        )
    )
}
