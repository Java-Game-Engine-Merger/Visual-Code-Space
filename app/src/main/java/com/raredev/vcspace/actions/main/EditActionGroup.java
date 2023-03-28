package com.raredev.vcspace.actions.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.raredev.vcspace.R;
import com.raredev.vcspace.actions.Action;
import com.raredev.vcspace.actions.ActionData;
import com.raredev.vcspace.actions.ActionGroup;
import com.raredev.vcspace.actions.main.edit.FindTextAction;
import com.raredev.vcspace.actions.main.edit.FormatAction;
import com.raredev.vcspace.activity.MainActivity;

public class EditActionGroup extends ActionGroup {

  public EditActionGroup() {
    location = Action.Location.MAIN_TOOLBAR;
  }

  @Override
  public void update(@NonNull ActionData data) {
    visible = false;
    var main = (MainActivity) data.get(MainActivity.class);

    if (main == null) {
      return;
    }
    if (main.getCurrentEditor() == null) {
      return;
    }
    visible = true;
    title = R.string.menu_edit;
    icon = R.drawable.pencil;
  }

  @Override
  public Action[] getChildren(@Nullable ActionData data) {
    return new Action[] {new FindTextAction(), new FormatAction()};
  }
}
