package mx.edu.itson.practica11_241400;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText txtid, txtnom;
    private Button btnbus, btnmod, btnreg, btneli;
    private ListView lvDatos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        txtid   = findViewById(R.id.txtid);
        txtnom  = findViewById(R.id.txtnom);
        btnbus  = findViewById(R.id.btnbus);
        btnmod  = findViewById(R.id.btnmod);
        btnreg  = findViewById(R.id.btnreg);
        btneli  = findViewById(R.id.btneli);
        lvDatos = findViewById(R.id.lvDatos);

        botonRegistrar();
        listarLuchadores();
        botonBuscar();
        botonModificar();
        botonEliminar();
    }

    private void botonRegistrar(){
        btnreg.setOnClickListener(view -> {
            String idStr = txtid.getText().toString().trim();
            String nom = txtnom.getText().toString().trim();

            if(idStr.isEmpty() || nom.isEmpty()){
                ocultarTeclado();
                Toast.makeText(MainActivity.this, "Complete Los Campos Faltantes!!", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    int id = Integer.parseInt(idStr);
                    Luchador luc = new Luchador(id, nom);

                    Log.d("FIREBASE_DEBUG", "Intentando registrar luchador: " + nom);

                    agregarLuchador(luc).addOnSuccessListener(suc -> {
                        Log.d("FIREBASE_DEBUG", "Luchador agregado con éxito");
                        ocultarTeclado();
                        Toast.makeText(MainActivity.this, "Luchador Agregado Correctamente!!", Toast.LENGTH_SHORT).show();
                        txtid.setText("");
                        txtnom.setText("");
                        txtid.requestFocus();
                    }).addOnFailureListener(err -> {
                        Log.e("FIREBASE_DEBUG", "Error al agregar: " + err.getMessage());
                        ocultarTeclado();
                        Toast.makeText(MainActivity.this, "Error: " + err.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "El ID debe ser un número", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Task<Void> agregarLuchador(Luchador luc){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference(Luchador.class.getSimpleName());
        return dbref.push().setValue(luc);
    }

    private void listarLuchadores(){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference(Luchador.class.getSimpleName());

        ArrayList<Luchador> lisluc = new ArrayList<>();
        ArrayAdapter<Luchador> ada = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, lisluc);
        lvDatos.setAdapter(ada);

        dbref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Luchador luc = snapshot.getValue(Luchador.class);
                if (luc != null) {
                    lisluc.add(luc);
                    ada.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ada.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Luchador removedLuc = snapshot.getValue(Luchador.class);
                if (removedLuc != null) {
                    lisluc.removeIf(l -> l.getId() == removedLuc.getId());
                    ada.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_DEBUG", "Error en listener: " + error.getMessage());
            }
        });

        lvDatos.setOnItemClickListener((adapterView, view, position, l) -> {
            ocultarTeclado();
            Luchador luc = lisluc.get(position);
            txtid.setText(String.valueOf(luc.getId()));
            txtnom.setText(luc.getNombre());
        });

        lvDatos.setOnItemLongClickListener((adapterView, view, position, l) -> {
            Luchador luc = lisluc.get(position);
            String aux = String.valueOf(luc.getId());

            FirebaseDatabase.getInstance().getReference(Luchador.class.getSimpleName())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot x : snapshot.getChildren()){
                            Object idVal = x.child("id").getValue();
                            if(idVal != null && idVal.toString().equalsIgnoreCase(aux)){
                                new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Pregunta")
                                    .setMessage("¿Está Seguro(a) De Querer Eliminar El Registro ("+aux+")?")
                                    .setCancelable(false)
                                    .setNegativeButton("Cancelar", null)
                                    .setPositiveButton("Aceptar", (dialogInterface, i) -> {
                                        x.getRef().removeValue();
                                        ocultarTeclado();
                                        Toast.makeText(MainActivity.this, "Registro ("+aux+") Eliminado Correctamente!!", Toast.LENGTH_SHORT).show();
                                        txtid.setText("");
                                        txtnom.setText("");
                                        txtid.requestFocus();
                                    }).show();
                                return;
                            }
                        }
                        Toast.makeText(MainActivity.this, "Error. Id ("+aux+") No Encontrado!!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            return true;
        });
    }

    private void botonBuscar(){
        btnbus.setOnClickListener(view -> {
            String aux = txtid.getText().toString().trim();
            if(aux.isEmpty()){
                ocultarTeclado();
                Toast.makeText(MainActivity.this, "Indique El Id Para Buscar!!", Toast.LENGTH_SHORT).show();
                txtid.requestFocus();
            } else {
                FirebaseDatabase.getInstance().getReference(Luchador.class.getSimpleName())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean found = false;
                            for(DataSnapshot x : snapshot.getChildren()){
                                Object idVal = x.child("id").getValue();
                                if(idVal != null && idVal.toString().equalsIgnoreCase(aux)){
                                    Object nomVal = x.child("nombre").getValue();
                                    txtnom.setText(nomVal != null ? nomVal.toString() : "");
                                    found = true;
                                    break;
                                }
                            }
                            if(!found){
                                ocultarTeclado();
                                Toast.makeText(MainActivity.this, "Error. Id ("+aux+") No Encontrado!!", Toast.LENGTH_SHORT).show();
                                txtnom.setText("");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
            }
        });
    }

    private void botonModificar(){
        btnmod.setOnClickListener(view -> {
            String idStr = txtid.getText().toString().trim();
            String nom = txtnom.getText().toString().trim();
            if(idStr.isEmpty() || nom.isEmpty()){
                ocultarTeclado();
                Toast.makeText(MainActivity.this, "Complete Los Campos Para Continuar!!!!", Toast.LENGTH_SHORT).show();
                txtid.requestFocus();
            } else {
                FirebaseDatabase.getInstance().getReference(Luchador.class.getSimpleName())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            DataSnapshot target = null;
                            boolean nameExists = false;
                            for(DataSnapshot x : snapshot.getChildren()){
                                Object idVal = x.child("id").getValue();
                                if(idVal != null && idVal.toString().equalsIgnoreCase(idStr)) target = x;
                                Object nomVal = x.child("nombre").getValue();
                                if(nomVal != null && nomVal.toString().equalsIgnoreCase(nom)) nameExists = true;
                            }

                            if(target == null){
                                Toast.makeText(MainActivity.this, "Error. Id ("+idStr+") No Encontrado!!", Toast.LENGTH_SHORT).show();
                            } else if(nameExists){
                                Toast.makeText(MainActivity.this, "Error. El Nombre ("+nom+") Ya Existe!!", Toast.LENGTH_SHORT).show();
                            } else {
                                final DataSnapshot finalTarget = target;
                                new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Pregunta")
                                    .setMessage("¿Está Seguro(a) De Querer Modificar El Nombre Del Registro ("+idStr+")?")
                                    .setCancelable(false)
                                    .setNegativeButton("Cancelar", null)
                                    .setPositiveButton("Aceptar", (dialogInterface, i) -> {
                                        finalTarget.getRef().child("nombre").setValue(nom);
                                        ocultarTeclado();
                                        Toast.makeText(MainActivity.this, "Dato Modificado Correctamente!!", Toast.LENGTH_SHORT).show();
                                        txtid.setText("");
                                        txtnom.setText("");
                                        txtid.requestFocus();
                                    }).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
            }
        });
    }

    private void botonEliminar(){
        btneli.setOnClickListener(view -> {
            String aux = txtid.getText().toString().trim();
            if(aux.isEmpty()){
                ocultarTeclado();
                Toast.makeText(MainActivity.this, "Indique El Id Para Eliminar!!", Toast.LENGTH_SHORT).show();
                txtid.requestFocus();
            } else {
                FirebaseDatabase.getInstance().getReference(Luchador.class.getSimpleName())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for(DataSnapshot x : snapshot.getChildren()){
                                Object idVal = x.child("id").getValue();
                                if(idVal != null && idVal.toString().equalsIgnoreCase(aux)){
                                    new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Pregunta")
                                        .setMessage("¿Está Seguro(a) De Querer Eliminar El Registro ("+aux+")?")
                                        .setCancelable(false)
                                        .setNegativeButton("Cancelar", null)
                                        .setPositiveButton("Aceptar", (dialogInterface, i) -> {
                                            x.getRef().removeValue();
                                            ocultarTeclado();
                                            Toast.makeText(MainActivity.this, "Registro ("+aux+") Eliminado Correctamente!!", Toast.LENGTH_SHORT).show();
                                            txtid.setText("");
                                            txtnom.setText("");
                                            txtid.requestFocus();
                                        }).show();
                                    return;
                                }
                            }
                            Toast.makeText(MainActivity.this, "Error. Id ("+aux+") No Encontrado!!", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
            }
        });
    }

    private void ocultarTeclado(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
