package com.raredev.vcspace.fragments.workspace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.raredev.vcspace.R
import com.raredev.vcspace.activities.NodeRedActivity
import com.raredev.vcspace.activities.SettingsActivity
import com.raredev.vcspace.activities.TerminalActivity
import com.raredev.vcspace.databinding.FragmentWorkspaceBinding
import com.raredev.vcspace.viewmodel.FileExplorerViewModel

class WorkspaceFragment : Fragment(), NavController.OnDestinationChangedListener {

  private val fileViewModel by
  viewModels<FileExplorerViewModel>(ownerProducer = { requireActivity() })

  private var _binding: FragmentWorkspaceBinding? = null
  private val binding: FragmentWorkspaceBinding
    get() = checkNotNull(_binding)

  private lateinit var navController: NavController

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentWorkspaceBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val navHostFragment =
      childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    navController = navHostFragment.navController
    NavigationUI.setupWithNavController(binding.navRail, navController)

    navController.addOnDestinationChangedListener(this)
    binding.navRail.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.menu_terminal ->
          TerminalActivity.startTerminalWithDir(requireContext(), fileViewModel.currentPath.value)

        R.id.menu_node_red ->
          NodeRedActivity.startTerminalWithDir(requireContext(), fileViewModel.currentPath.value)

        R.id.menu_settings -> startActivity(Intent(requireContext(), SettingsActivity::class.java))
      }
      false
    }
  }

  override fun onDestinationChanged(
    controller: NavController,
    destination: NavDestination,
    arguments: Bundle?
  ) {
    binding.navRail.menu.forEach { item ->
      if (destination.id == item.itemId) {
        binding.title.text = item.title
        item.isChecked = true
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    navController.removeOnDestinationChangedListener(this)
    _binding = null
  }
}
