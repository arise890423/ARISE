package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.*
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.*

@Composable
fun ShopScreen(
    profile: HunterProfile,
    shopItems: List<ShopItem>,
    purchaseHistory: List<PurchaseRecord> = emptyList(),
    onBuyItem: (ShopItem) -> PurchaseResult,
    onEquipItem: (ShopItem) -> Unit,
    onUnequipItem: (ShopItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    var shopTab by remember { mutableStateOf(0) } // 0: Catalog, 1: My Vault (Inventory), 2: Purchase History
    var selectedCategory by remember { mutableStateOf<ShopCategory?>(null) }
    var detailItem by remember { mutableStateOf<ShopItem?>(null) }
    var purchaseErrorMessage by remember { mutableStateOf<String?>(null) }

    val ownedItems = remember(shopItems) { shopItems.filter { it.isPurchased } }

    val displayedItems = remember(shopItems, shopTab, selectedCategory) {
        when (shopTab) {
            1 -> {
                if (selectedCategory == null) ownedItems else ownedItems.filter { it.category == selectedCategory }
            }
            0 -> {
                if (selectedCategory == null) shopItems else shopItems.filter { it.category == selectedCategory }
            }
            else -> emptyList()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("shop_screen_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Balance Header Card
        item {
            SystemWindowCard(borderColor = GoldPrimary.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SYSTEM STORE & ARSENAL",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = GoldGlow,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = profile.rank.color.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, profile.rank.color.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = profile.rank.code,
                                        color = profile.rank.color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Exchange dungeon gold and mana crystals for elite cosmetics and artifacts.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = SystemSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Dungeon Gold", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "🪙 ${profile.gold}",
                                    color = GoldGlow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = SystemSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ManaBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Mana Crystals", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "💎 ${profile.crystals}",
                                    color = ManaBlue,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Tabs: Catalog, Vault, Purchase History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(0, "Store Catalog", "catalog_tab"),
                    Triple(1, "My Vault (${ownedItems.size})", "vault_tab"),
                    Triple(2, "Order History", "history_tab")
                ).forEach { (idx, label, testTag) ->
                    val isSelected = shopTab == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { shopTab = idx }
                            .testTag(testTag),
                        color = if (isSelected) accent.copy(alpha = 0.2f) else SystemSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) accent else SystemCardBorder)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) accent else TextSecondary
                            ),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // Category Filter Row (for Catalog and Vault)
        if (shopTab != 2) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Items", fontSize = 11.sp) }
                        )
                    }
                    items(ShopCategory.entries.toTypedArray()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Content Rendering
        when (shopTab) {
            2 -> {
                // Purchase History View
                if (purchaseHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No past purchase receipts found in System logs.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "SYSTEM TRANSACTION LOGS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    items(purchaseHistory, key = { it.id }) { record ->
                        PurchaseHistoryRow(record = record)
                    }
                }
            }
            else -> {
                // Catalog or Vault
                if (displayedItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (shopTab == 1) "Your inventory is currently empty for this category." else "No items found.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(displayedItems, key = { it.id }) { item ->
                        ShopItemCard(
                            item = item,
                            profile = profile,
                            isVaultMode = shopTab == 1,
                            onCardClick = { detailItem = item },
                            onEquip = { onEquipItem(item) },
                            onUnequip = { onUnequipItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Item Detail & Purchase Confirmation Dialog
    detailItem?.let { item ->
        ItemDetailAndPurchaseDialog(
            item = item,
            profile = profile,
            onDismiss = {
                detailItem = null
                purchaseErrorMessage = null
            },
            onConfirmPurchase = {
                val res = onBuyItem(item)
                when (res) {
                    is PurchaseResult.Success -> {
                        detailItem = null
                        purchaseErrorMessage = null
                    }
                    is PurchaseResult.Error -> {
                        purchaseErrorMessage = res.message
                    }
                }
            },
            onEquip = {
                onEquipItem(item)
                detailItem = null
            },
            onUnequip = {
                onUnequipItem(item)
                detailItem = null
            },
            errorMessage = purchaseErrorMessage
        )
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    profile: HunterProfile,
    isVaultMode: Boolean,
    onCardClick: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val rankLocked = profile.level < item.requiredRank.minLevel

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("shop_item_${item.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SystemSurface),
        border = BorderStroke(
            1.dp,
            when {
                item.isEquipped -> accent
                item.isPurchased -> SuccessGreen.copy(alpha = 0.5f)
                else -> SystemCardBorder
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Preview Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SystemSurfaceVariant)
                    .border(
                        1.dp,
                        if (item.isEquipped) accent else SystemCardBorder,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.category == ShopCategory.THEMES -> {
                        val swatchColor = when (item.id) {
                            "VIOLET" -> MonarchPurple
                            "CRIMSON" -> CrimsonPrimary
                            "GOLD" -> GoldPrimary
                            else -> CyanPrimary
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                    item.category == ShopCategory.FRAMES -> {
                        val frameBorderColor = when (item.id) {
                            "frame_monarch" -> MonarchPurple
                            "frame_crimson" -> CrimsonPrimary
                            "frame_sovereign" -> GoldPrimary
                            else -> CyanPrimary
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(3.dp, frameBorderColor, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    item.category == ShopCategory.TITLES -> {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = item.name,
                            tint = GoldGlow,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    item.iconRes != null -> {
                        Image(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = item.name,
                            tint = accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    if (item.requiredRank != HunterRank.E) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = item.requiredRank.color.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = item.requiredRank.code,
                                color = item.requiredRank.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = item.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )

                if (item.statBonus.isNotBlank()) {
                    Text(
                        text = "Bonus: ${item.statBonus}",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!item.isPurchased) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.costGold > 0) {
                            Text(
                                text = "🪙 ${item.costGold}G",
                                color = GoldGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item.costCrystals > 0) {
                            Text(
                                text = "💎 ${item.costCrystals}",
                                color = ManaBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item.costGold == 0 && item.costCrystals == 0) {
                            Text(
                                text = "FREE",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Status Area
            when {
                rankLocked -> {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.requiredRank.code,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item.isEquipped -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = accent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Text(
                                text = "EQUIPPED",
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (isVaultMode && item.category != ShopCategory.EQUIPMENT) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unequip",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable { onUnequip() }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
                item.isPurchased -> {
                    OutlinedButton(
                        onClick = onEquip,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SuccessGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("equip_button_${item.id}")
                    ) {
                        Text(text = "EQUIP", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    val canAfford = profile.gold >= item.costGold && profile.crystals >= item.costCrystals
                    Button(
                        onClick = onCardClick,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) GoldPrimary else SystemSurfaceVariant,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("buy_button_${item.id}")
                    ) {
                        Text(text = "VIEW", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDetailAndPurchaseDialog(
    item: ShopItem,
    profile: HunterProfile,
    onDismiss: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
    errorMessage: String? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val rankLocked = profile.level < item.requiredRank.minLevel
    val canAfford = profile.gold >= item.costGold && profile.crystals >= item.costCrystals
    val eligibleToBuy = !rankLocked && canAfford && !item.isPurchased

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SystemSurface),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Tag
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ITEM SPECIFICATION: ${item.category.title.uppercase()}",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Item Name
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                )

                // Required Rank
                if (item.requiredRank != HunterRank.E) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = item.requiredRank.color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, item.requiredRank.color.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "REQUIRED: ${item.requiredRank.code} (Lv ${item.requiredRank.minLevel})",
                            color = item.requiredRank.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Large Preview Area
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SystemSurfaceVariant)
                        .border(1.dp, SystemCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        item.category == ShopCategory.THEMES -> {
                            val swatchColor = when (item.id) {
                                "VIOLET" -> MonarchPurple
                                "CRIMSON" -> CrimsonPrimary
                                "GOLD" -> GoldPrimary
                                else -> CyanPrimary
                            }
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            )
                        }
                        item.category == ShopCategory.FRAMES -> {
                            val frameColor = when (item.id) {
                                "frame_monarch" -> MonarchPurple
                                "frame_crimson" -> CrimsonPrimary
                                "frame_sovereign" -> GoldPrimary
                                else -> CyanPrimary
                            }
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .border(4.dp, frameColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        item.iconRes != null -> {
                            Image(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = item.name,
                                tint = GoldGlow,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description & Lore
                Text(
                    text = item.description,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                if (item.lore.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${item.lore}\"",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }

                if (item.statBonus.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Effect: ${item.statBonus}",
                            color = SuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Financial Breakdown (Price & Balances)
                if (!item.isPurchased) {
                    SystemWindowCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (canAfford) GoldPrimary.copy(alpha = 0.4f) else CrimsonPrimary.copy(alpha = 0.4f)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Price Tag", color = TextSecondary, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.costGold > 0) Text("🪙 ${item.costGold}G", color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (item.costCrystals > 0) Text("💎 ${item.costCrystals}", color = ManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (item.costGold == 0 && item.costCrystals == 0) Text("FREE", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Balance", color = TextSecondary, fontSize = 11.sp)
                                Text("🪙 ${profile.gold}G • 💎 ${profile.crystals}", color = Color.White, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = SystemCardBorder, modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining", color = TextSecondary, fontSize = 11.sp)
                                val remainingGold = profile.gold - item.costGold
                                val remainingCrystals = profile.crystals - item.costCrystals
                                Text(
                                    text = "🪙 ${remainingGold}G • 💎 $remainingCrystals",
                                    color = if (remainingGold >= 0 && remainingCrystals >= 0) SuccessGreen else CrimsonPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Error Message if any
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = CrimsonPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SystemCardBorder)
                    ) {
                        Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
                    }

                    when {
                        item.isEquipped -> {
                            Button(
                                onClick = onUnequip,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("UNEQUIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        item.isPurchased -> {
                            Button(
                                onClick = onEquip,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("EQUIP NOW", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onConfirmPurchase,
                                enabled = eligibleToBuy,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (eligibleToBuy) GoldPrimary else SystemSurfaceVariant,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (rankLocked) "RANK LOCKED" else if (!canAfford) "SHORT FUNDS" else "CONFIRM BUY",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseHistoryRow(record: PurchaseRecord, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SystemSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SystemCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.itemName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${record.category.title} • ${record.formattedDate}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (record.costGold > 0) {
                        Text(text = "🪙 ${record.costGold}G", color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (record.costCrystals > 0) {
                        Text(text = "💎 ${record.costCrystals}", color = ManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (record.costGold == 0 && record.costCrystals == 0) {
                        Text(text = "FREE", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = record.status,
                        color = SuccessGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
