#include <iostream>
#include <stdexcept>
#include <sstream>

#include <boost/filesystem.hpp>

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <string.h>
#include <assert.h>

int main(int argc, char **argv) {
	// Starting APIserver mock object
	boost::filesystem::path ep = boost::filesystem::current_path() / argv[0];
	boost::filesystem::path apiMockPath = ep.parent_path() / ".." / ".." / "python" / "test" / "mock_api" / "apiserver_mock.py";
	boost::filesystem::path apiMockHandlerPath = ep.parent_path() / ".." / ".." / "python" / "test" / "mock_api" / "test_retry.py";

	std::cerr << "Blocking SIGCHLD" << std::endl;
        sigset_t mask, omask;
        sigemptyset(&mask);
        sigaddset(&mask, SIGCHLD);
        sigprocmask(SIG_BLOCK, &mask, &omask);

	std::cerr << "Forking" << std::endl;
        pid_t apiMockPid = fork();
        assert(apiMockPid >= 0);
	if (apiMockPid == 0) {
		std::cerr << "Unlocking SIGCHLD in child" << std::endl;
		// Child process
		sigprocmask(SIG_SETMASK, &omask, NULL);

		// Launching API mock object (Method 1)
		//std::ostringstream cmd;
		//cmd << "python " << apiMockPath.string() << ' ' << apiMockHandlerPath.string();
		//execl("/bin/sh", "sh", "-c", cmd.str().c_str(), static_cast<char *>(0));

		// Launching API mock object (Method 2)
		execl("/bin/sh", "python", apiMockPath.string().c_str(), apiMockHandlerPath.string().c_str(), static_cast<char *>(0));
		
		std::ostringstream msg;
		msg << "Error launching API mock object: " << strerror(errno);
		throw std::runtime_error(msg.str());
	}
	std::cerr << "API mock object started with pid " << apiMockPid << std::endl;
	// Await for API mock object to start
	//usleep(1000000000);

	//sleep(1);
	sleep(500);

        std::cerr << "Sending SIGTERM to API mock object using pid " << apiMockPid << std::endl;
        if (kill(apiMockPid, SIGTERM) != 0) {
            throw std::runtime_error(strerror(errno));
        }
        std::cerr << "Awaiting for API mock to stop pid " << apiMockPid << std::endl;
        int status;
        pid_t p = waitpid(apiMockPid, &status, 0);
        assert(p == apiMockPid);
        assert(WEXITSTATUS(status) == 0);
	std::cerr << "Unlocking SIGCHLD in parent" << std::endl;
        sigprocmask(SIG_SETMASK, &omask, NULL);
}
