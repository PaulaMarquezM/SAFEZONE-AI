package com.example.safezoneai.ui

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.local.EmergencyContact
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel

/**
 * Limpia un numero telefonico removiendo caracteres no validos.
 * Mantiene solo digitos y el signo + al inicio.
 */
fun cleanPhoneNumber(phone: String): String {
    return phone.replace(Regex("[^+\\d]"), "")
}


/**
 * ðŸ“± PANTALLA DE CONTACTOS DE EMERGENCIA
 *
 * Permite al usuario:
 * - Ver lista de contactos guardados
 * - Agregar nuevos contactos
 * - Editar contactos existentes
 * - Eliminar contactos
 * - Marcar contactos como principales
 *
 * ARQUITECTURA: MVVM
 * - View: Esta screen
 * - ViewModel: EmergencyViewModel
 * - Model: EmergencyContact + EmergencyDao
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onBack: () -> Unit
) {
    // Estados reactivos
    val emergencyContacts by viewModel.emergencyContacts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Contactos de Emergencia",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeGreen,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SafeGreen,
                contentColor = PureWhite
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, "Agregar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SafeGreenLight.copy(alpha = 0.3f),
                            PureWhite
                        )
                    )
                )
                .padding(padding)
        ) {
            if (emergencyContacts.isEmpty()) {
                EmptyContactsState(onAddClick = { showAddDialog = true })
            } else {
                ContactsList(
                    contacts = emergencyContacts,
                    onEdit = { contactToEdit = it },
                    onDelete = { contactToDelete = it }
                )
            }
        }
    }


    if (showAddDialog || contactToEdit != null) {
        ContactFormDialog(
            contact = contactToEdit,
            onDismiss = {
                showAddDialog = false
                contactToEdit = null
            },
            onSave = { contact ->
                if (contactToEdit != null) {
                    viewModel.updateEmergencyContact(contact)
                } else {
                    viewModel.addEmergencyContact(contact)
                }
                showAddDialog = false
                contactToEdit = null
            }
        )
    }

    if (contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Eliminar contacto",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Seguro que deseas eliminar a ${contactToDelete?.name}?\n\nEsta acción no se puede deshacer.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        contactToDelete?.let { viewModel.deleteEmergencyContact(it) }
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyContactsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ContactPhone,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = TextSecondary.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No tienes contactos de emergencia",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Agrega al menos un contacto para recibir alertas en caso de emergencia.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Agregar Primer Contacto",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Composable
fun ContactsList(
    contacts: List<EmergencyContact>,
    onEdit: (EmergencyContact) -> Unit,
    onDelete: (EmergencyContact) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InfoCard()
        }

        items(contacts, key = { it.id }) { contact ->
            ContactCard(
                contact = contact,
                onEdit = { onEdit(contact) },
                onDelete = { onDelete(contact) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun InfoCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SafeGreenLight.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = SafeGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Estos contactos recibirán un SMS con tu ubicación cuando actives el botón de emergencia.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark
            )
        }
    }
}

@Composable
fun ContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.isPrimary)
                SafeGreen.copy(alpha = 0.1f)
            else
                PureWhite
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                SafeGreen.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            contact.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            contact.relationship,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                if (contact.isPrimary) {
                    Surface(
                        color = SafeGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // NÃºmero telefÃ³nico
            Surface(
                color = SafeGreenLight.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        contact.phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acciÃ³n
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SafeGreen
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DangerRed
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "Familiar") }
    var isPrimary by remember { mutableStateOf(contact?.isPrimary ?: false) }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val relationships = listOf("Familiar", "Amigo/a", "Policía", "Otro")
    var expanded by remember { mutableStateOf(false) }

    // Launcher para importar contacto desde la agenda del telefono
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    context.contentResolver.query(
                        uri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val pickedName = cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                                )
                            )
                            val pickedPhone = cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                                )
                            )
                            if (!pickedName.isNullOrBlank()) name = pickedName
                            if (!pickedPhone.isNullOrBlank()) phone = cleanPhoneNumber(pickedPhone)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Titulo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (contact == null) Icons.Default.PersonAdd else Icons.Default.Edit,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (contact == null) "Agregar Contacto" else "Editar Contacto",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Boton: Importar de la agenda
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                        )
                        contactPickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeGreen)
                ) {
                    Icon(
                        Icons.Default.ContactPhone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Importar de la agenda",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeGreen,
                        focusedLabelColor = SafeGreen,
                        cursorColor = SafeGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: Telefono
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numero telefonico") },
                    placeholder = { Text("+593999999999") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeGreen,
                        focusedLabelColor = SafeGreen,
                        cursorColor = SafeGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo: RelaciÃ³n (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relación") },
                        leadingIcon = {
                            Icon(Icons.Default.Group, null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafeGreen,
                            focusedLabelColor = SafeGreen
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        relationships.forEach { rel ->
                            DropdownMenuItem(
                                text = { Text(rel) },
                                onClick = {
                                    relationship = rel
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch: Contacto principal
                Surface(
                    color = SafeGreenLight.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Contacto principal",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Se notifica primero",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = isPrimary,
                            onCheckedChange = { isPrimary = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = SafeGreen
                            )
                        )
                    }
                }

                // Mensaje de error
                AnimatedVisibility(visible = showError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = DangerRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                errorMessage,
                                color = DangerRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val cleanedPhone = cleanPhoneNumber(phone)
                            when {
                                name.isBlank() -> {
                                    showError = true
                                    errorMessage = "El nombre no puede estar vacio"
                                }
                                cleanedPhone.isBlank() -> {
                                    showError = true
                                    errorMessage = "El telefono no puede estar vacio"
                                }
                                cleanedPhone.replace("+", "").length < 10 -> {
                                    showError = true
                                    errorMessage = "Numero telefonico muy corto (min 10 digitos)"
                                }
                                else -> {
                                    onSave(
                                        EmergencyContact(
                                            id = contact?.id ?: 0,
                                            name = name.trim(),
                                            phoneNumber = cleanedPhone,
                                            relationship = relationship,
                                            isPrimary = isPrimary
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                    ) {
                        Text("Guardar", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}